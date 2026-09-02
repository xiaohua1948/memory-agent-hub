package com.memory.xiaohua.ai.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.ai.action.PendingActionService;
import com.memory.xiaohua.ai.tool.AlbumTools;
import com.memory.xiaohua.ai.tool.QueryTools;
import com.memory.xiaohua.ai.tool.WebpageTools;
import com.memory.xiaohua.model.vo.ai.PendingActionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Agent 编排器 —— 有界 tool-use 编排循环
 *
 * <p>职责：
 * <ol>
 *   <li>调用 {@link RouterAgent} 拆解执行计划；</li>
 *   <li>按依赖顺序执行子任务，子任务间通过 {@link AgentContext} 透传中间结果；</li>
 *   <li>写操作短路：生成 PendingAction 交前端确认，不落库；</li>
 *   <li>读操作（查询 / 生成网页）直接执行；</li>
 *   <li>结果通过回调 {@code sink} 流式推送（文本 / 卡片 / HTML）。</li>
 * </ol>
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class AgentOrchestrator {

    private final RouterAgent routerAgent;
    private final QueryTools queryTools;
    private final AlbumTools albumTools;
    private final WebpageTools webpageTools;
    private final PendingActionService pendingActionService;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(RouterAgent routerAgent,
                             QueryTools queryTools,
                             AlbumTools albumTools,
                             WebpageTools webpageTools,
                             PendingActionService pendingActionService,
                             ObjectMapper objectMapper) {
        this.routerAgent = routerAgent;
        this.queryTools = queryTools;
        this.albumTools = albumTools;
        this.webpageTools = webpageTools;
        this.pendingActionService = pendingActionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行一轮对话。
     *
     * @param context 编排上下文（userId/sessionId/用户输入/照片上下文）
     * @param sink    结果回调（type: text / card / html；data: 内容）
     */
    public void execute(AgentContext context, Consumer<Map<String, Object>> sink) {
        List<AgentPlan.Step> steps = resolvePlan(context);
        if (steps.isEmpty()) {
            // 无计划：交给主模型做自由问答（简化处理，输出通用回复）
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("type", "text");
            msg.put("data", "请更具体地描述你的需求，例如：查询照片、创建相册、生成网页。");
            sink.accept(msg);
            return;
        }

        Map<Integer, Object> stepOutput = new LinkedHashMap<>();
        for (AgentPlan.Step step : steps) {
            Map<String, Object> msg = new LinkedHashMap<>();
            switch (step.getAgent().toUpperCase()) {
                case "QUERY" -> {
                    List<Map<String, Object>> photos = queryTools.searchPhotosStructured(step.getInput(), context.getUserId());
                    stepOutput.put(step.getStep(), photos);
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("type", "text");
                    summary.put("data", photos.isEmpty() ? "未找到符合条件的照片" : "找到 " + photos.size() + " 张照片：");
                    sink.accept(summary);
                    msg.put("type", "photos");
                    msg.put("data", photos);
                }
                case "ALBUM" -> {
                    PendingActionVO vo = buildAlbumPendingAction(step, context, stepOutput);
                    String actionId = pendingActionService.createPendingAction(
                            context.getUserId(), context.getSessionId(),
                            vo.getActionType(), toExecParams(step, context, stepOutput), vo);
                    vo.setActionId(actionId);
                    msg.put("type", "card");
                    msg.put("data", vo);
                }
                case "CODE" -> {
                    String html = webpageTools.generateHtml(
                            context.getContextFileIds(), step.getInput(), context.getUserId());
                    msg.put("type", "html");
                    msg.put("data", html);
                }
                default -> {
                    msg.put("type", "text");
                    msg.put("data", "未知任务类型: " + step.getAgent());
                }
            }
            sink.accept(msg);
        }
    }

    /** 解析执行计划（Router 输出 JSON → List<Step>，携带历史对话以支持多轮指代） */
    private List<AgentPlan.Step> resolvePlan(AgentContext context) {
        try {
            String json = routerAgent.route(context.getUserInput(), context.getHistory());
            if (json == null || json.isBlank()) {
                return List.of();
            }
            json = extractJsonArray(json);
            List<Map<String, Object>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            List<AgentPlan.Step> steps = new ArrayList<>();
            for (Map<String, Object> item : raw) {
                AgentPlan.Step s = new AgentPlan.Step();
                s.setStep(((Number) item.get("step")).intValue());
                s.setAgent(String.valueOf(item.get("agent")));
                s.setInput(String.valueOf(item.getOrDefault("input", "")));
                Object dep = item.get("dependsOn");
                s.setDependsOn(dep == null ? null : ((Number) dep).intValue());
                s.setWrite(Boolean.TRUE.equals(item.get("write")));
                steps.add(s);
            }
            return steps;
        } catch (Exception e) {
            log.warn("[AI] Router 解析失败，回退单步处理: {}", e.getMessage());
            // 回退：视为相册/查询复合意图无法识别，输出空（走通用回复）
            return List.of();
        }
    }

    private String extractJsonArray(String s) {
        String t = s.trim();
        int start = t.indexOf('[');
        int end = t.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return "[]";
    }

    /** 根据相册子任务构造确认卡片 */
    private PendingActionVO buildAlbumPendingAction(AgentPlan.Step step, AgentContext context,
                                                    Map<Integer, Object> stepOutput) {
        PendingActionVO vo = new PendingActionVO();
        String input = step.getInput();
        vo.setActionType(detectActionType(input));
        vo.setTitle(vo.getActionType().equals("CREATE_ALBUM") ? "创建相册" : "相册操作");
        vo.setDescription("即将执行：" + input);
        // 携带上游查询结果（若有）
        vo.setPreview(Map.of("input", input));
        return vo;
    }

    private String detectActionType(String input) {
        if (input.contains("删除") || input.contains("移除")) return "DELETE_ALBUM";
        if (input.contains("创建") || input.contains("新建")) return "CREATE_ALBUM";
        if (input.contains("添加") || input.contains("加入")) return "ADD_PICTURES";
        return "UPDATE_ALBUM";
    }

    /** 生成执行参数 JSON（供确认后执行） */
    private String toExecParams(AgentPlan.Step step, AgentContext context, Map<Integer, Object> stepOutput) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("actionType", detectActionType(step.getInput()));
        params.put("input", step.getInput());
        params.put("userId", context.getUserId());
        // 若有上游依赖步骤，透传其结果（简化：记录依赖）
        if (step.getDependsOn() != null) {
            params.put("dependsOn", step.getDependsOn());
            params.put("upstreamResult", String.valueOf(stepOutput.get(step.getDependsOn())));
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }
}
