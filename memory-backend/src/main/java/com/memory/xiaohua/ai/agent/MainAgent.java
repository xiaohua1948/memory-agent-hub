package com.memory.xiaohua.ai.agent;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.tool.AgentTools;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 主 Agent —— 基于 LLM function calling 的自由编排器
 *
 * <p>取代旧「Router 拆计划 + 硬编码 switch 执行」模式：主模型在每一轮自主决定
 * 「调用哪个子 Agent 工具、传什么参数」，工具结果回填对话后继续推理，直到产出
 * 最终答复（ReAct 循环）。编排智能由模型承担，而非写死的计划模板，因此能处理
 * 任意组合的复杂任务，例如：
 * <pre>
 * 「查询地点为上海且大小2MB以上的照片，创建名为『上海美景』的相册，
 *   描述为：上海照片里大于2mb的照片」
 * </pre>
 * 主 Agent 会自动编排为：searchPhotos（拿到真实 fileId）→ createAlbum（名称/描述/照片列表）。
 *
 * <p>执行语义：
 * <ul>
 *   <li>读操作（查询/网页）工具直接执行；</li>
 *   <li>写操作（相册）工具只生成「草稿 + 确认卡片」（human-in-the-loop），不落库；</li>
 *   <li>工具抛出的业务异常信息回传给模型，模型可自我修正参数后重试；</li>
 *   <li>迭代上限由 {@code ai.max-tool-iterations} 控制（有界循环）。</li>
 * </ul>
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class MainAgent {

    private final AgentTools agentTools;
    private final AiProperties properties;

    @Autowired(required = false)
    @Qualifier("mainModel")
    private ChatModel mainModel;

    /** 工具名 → 执行器（启动时从 @Tool 注解自动提取规格并装配） */
    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();

    /** 工具规格列表：每次请求必须随 ChatRequest.parameters 传给 LLM，否则模型不知道工具存在 */
    private final List<ToolSpecification> toolSpecifications = new ArrayList<>();

    /**
     * 步骤序号（原子自增，前端按 id 定位并更新步骤状态）。
     *
     * <p>不随每次请求重置：并发请求下重置反而更容易撞号，
     * 而步骤 id 只在当次 SSE 流内使用，跨请求递增不影响展示。
     */
    private final java.util.concurrent.atomic.AtomicInteger stepSeq =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public MainAgent(AgentTools agentTools, AiProperties properties) {
        this.agentTools = agentTools;
        this.properties = properties;
        // 扫描 @Tool 注解方法：规格用于请求参数（tool schema），Method 用于构建执行器
        for (java.lang.reflect.Method method : agentTools.getClass().getMethods()) {
            if (method.getAnnotation(dev.langchain4j.agent.tool.Tool.class) == null) {
                continue;
            }
            ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
            toolSpecifications.add(spec);
            toolExecutors.put(spec.name(), new DefaultToolExecutor(agentTools, method));
        }
        log.info("[AI][MainAgent] 注册子Agent工具: {}", toolExecutors.keySet());
    }

    /**
     * 执行一轮对话编排。
     *
     * @param context 编排上下文（userId/sessionId/用户输入/历史/照片上下文）
     * @param sink    SSE 事件回调（type: text/tool/photos/card/html）
     */
    public void execute(AgentContext context, Consumer<Map<String, Object>> sink) {
        if (mainModel == null) {
            pushText(sink, "AI 服务未启用，请在 application.yml 配置 ai.api-key 后重试。");
            return;
        }

        List<ChatMessage> messages = buildMessages(context);
        int maxIterations = Math.max(1, properties.getMaxToolIterations());
        int nudgeCount = 0;

        // 绑定工具执行上下文（userId 注入 + 历史照片复用 + SSE 进度推送），循环结束在 finally 中清理
        AgentToolContext.bind(context.getUserId(), context.getSessionId(),
                context.getContextFileIds(), context.getRecentPhotoIds(), sink);
        try {
            for (int round = 0; round < maxIterations; round++) {
                // 关键：工具规格必须通过 ChatRequest.parameters 显式传递，
                // chat(List<ChatMessage>) 重载不携带 tools，模型将完全不知道工具存在
                ChatResponse response = mainModel.chat(dev.langchain4j.model.chat.request.ChatRequest.builder()
                        .messages(messages)
                        .parameters(dev.langchain4j.model.chat.request.ChatRequestParameters.builder()
                                .toolSpecifications(toolSpecifications)
                                .build())
                        .build());
                AiMessage aiMessage = response.aiMessage();
                messages.add(aiMessage);

                // 无工具调用：模型认为任务完成，输出最终答复
                if (!aiMessage.hasToolExecutionRequests()) {
                    String text = aiMessage.text();
                    // 兜底：模型输出"请稍候/正在处理"类过渡语却未发起工具调用——任务只是被
                    // "口头承诺"而未真正执行（常见于多步编排的中途轮次，如查询成功后
                    // 该调写工具时）。追加催促消息逼其继续调用工具（最多2次，防死循环）。
                    if (nudgeCount < 2 && isTransitionText(text)) {
                        nudgeCount++;
                        log.info("[AI][MainAgent] 检测到过渡语（第{}次）未发起工具调用，追加催促", nudgeCount);
                        messages.add(UserMessage.from(
                                "你上一条回复只是过渡性话语，任务还没有真正执行（没有发起任何工具调用）。"
                                        + "请对照用户需求检查还缺少哪些步骤（如创建相册、添加照片等写操作），"
                                        + "并立即发起相应的工具调用；所有步骤完成后才给出包含真实结果的最终总结。"));
                        continue;
                    }
                    pushText(sink, text);
                    return;
                }

                // 执行本轮全部工具调用，结果逐条回填（支持模型一次并行发起多个工具）
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    int stepId = stepSeq.incrementAndGet();
                    pushStep(sink, stepId, "running", ToolLabels.label(request.name(), request.arguments()), null);
                    // 把步骤号注入上下文：写操作生成待确认卡片时会带上它，
                    // 用户确认后前端才能精确地把对应步骤标记为完成
                    AgentToolContext.current().setCurrentStepId(stepId);
                    String result;
                    boolean failed = false;
                    try {
                        ToolExecutor executor = toolExecutors.get(request.name());
                        if (executor == null) {
                            result = "未知工具: " + request.name() + "，可用工具: " + toolExecutors.keySet();
                            failed = true;
                        } else {
                            result = executor.execute(request, context.getSessionId());
                        }
                    } catch (Exception e) {
                        // 取根因消息回传给模型，让其自我修正（如参数不合法、照片不存在）
                        Throwable root = e;
                        while (root.getCause() != null) {
                            root = root.getCause();
                        }
                        failed = true;
                        log.warn("[AI][MainAgent] 工具 {} 执行失败: {}", request.name(), root.getMessage());
                        result = "工具执行失败：" + root.getMessage() + "。请修正参数后重试，或调整策略。";
                    }
                    // 写操作只生成待确认草稿，步骤停在 waiting_confirm，等用户确认后由前端转为完成
                    String status = failed ? "failed"
                            : (ToolLabels.isWriteTool(request.name()) ? "waiting_confirm" : "done");
                    pushStep(sink, stepId, status, null, ToolLabels.firstLine(result, 40));
                    messages.add(ToolExecutionResultMessage.from(request, result == null ? "" : result));
                }
            }
            // 有界保护：超出迭代上限
            pushText(sink, "该任务较复杂，已达到最大执行步数（" + maxIterations
                    + "），已尽力完成部分工作；请分步描述需求后继续。");
        } finally {
            AgentToolContext.clear();
        }
    }

    // ==================== 内部方法 ====================

    /** 组装首轮消息：系统提示词 + 上下文摘要 + 历史对话（解析为消息序列）+ 本轮用户输入 */
    private List<ChatMessage> buildMessages(AgentContext context) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(buildSystemPrompt()));
        String contextNote = buildContextNote(context);
        if (!contextNote.isEmpty()) {
            messages.add(SystemMessage.from(contextNote));
        }
        messages.addAll(parseHistory(context.getHistory()));
        messages.add(UserMessage.from(context.getUserInput()));
        return messages;
    }

    /**
     * 上下文摘要：告知模型当前会话已有的照片资源，使其能直接复用而不重复查询。
     */
    private String buildContextNote(AgentContext context) {
        StringBuilder sb = new StringBuilder();

        // 用户在界面勾选的照片（最高优先级）：必须给出真实 fileId，否则模型无从使用
        if (context.getContextFileIds() != null && !context.getContextFileIds().isEmpty()) {
            sb.append("【用户当前选中的照片】共 ").append(context.getContextFileIds().size()).append(" 张，")
              .append("fileId 列表：").append(context.getContextFileIds()).append("\n")
              .append("使用方式（二选一）：\n")
              .append("1) 把上述列表原样写入工具的 fileIdsJson 参数；\n")
              .append("2) 或 fileIdsJson 传 [] —— 系统会自动改用用户选中的这批照片。\n")
              .append("**不要**为了确认用户选中的照片而去调用 searchPhotos：ID 已在上面给出，直接传入即可。\n");
        }

        // 本会话最近一次查询出的照片（多轮复用）
        if (context.getRecentPhotoIds() != null && !context.getRecentPhotoIds().isEmpty()) {
            sb.append("【本会话最近查询到的照片】共 ").append(context.getRecentPhotoIds().size()).append(" 张，")
              .append("fileId 列表：").append(context.getRecentPhotoIds()).append("\n")
              .append("用户说「这些照片/把它们/用这些」时指的就是它们，直接复用即可，")
              .append("**无需再次调用 searchPhotos**。只有用户提出全新筛选条件时才重新查询。\n");
        }
        return sb.toString();
    }

    /**
     * 过渡语检测：模型输出了"即将/正在/请稍候"类话语，但不含任何完成标志词——
     * 说明任务只是被"口头承诺"而未真正执行（未发起工具调用就停止）。
     *
     * <p>判断标准是「完成标志词豁免」而非文本长度：多步任务中途断链时，模型可能
     * 输出很长的 markdown"操作预览"（如把待办事项排成列表），长度豁免会漏判。
     */
    private boolean isTransitionText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String t = text.trim();
        // 含完成标志词 → 视为基于真实工具结果的正常总结答复，放行
        if (t.matches(".*(已创建|已生成|已放入|已添加|已删除|已修改|已更新|已移除|已执行|已将|已完成|成功|已把).*")) {
            return false;
        }
        // 过渡语特征：未来时/进行时承诺 + 等待暗示
        return t.contains("请稍候") || t.contains("请稍等") || t.contains("稍候") || t.contains("稍等")
                || t.contains("正在为您") || t.contains("正在为你") || t.contains("即将")
                || t.contains("马上") || t.contains("让我来") || t.contains("让我查") || t.contains("让我看看")
                || (t.contains("正在") && t.length() <= 150);
    }

    private String buildSystemPrompt() {
        return """
                你是「智能相册助手」的主 Agent，负责理解用户意图，并编排一组子 Agent 工具完成复杂任务。

                ## 输出纪律（最重要）
                - 需要执行操作时，**立即发起工具调用**；发起工具调用的那一轮不要输出任何正文。
                - 严禁输出"请稍候 / 正在为您查询 / 马上处理"之类过渡话语后停止——那会被视为任务结束。
                - 只有两种合法输出：①工具调用；②基于工具真实结果的最终总结答复。
                - 最终答复只能在所有工具执行完毕后给出。

                ## 可用工具（子 Agent 能力）
                - searchPhotos：照片查询子Agent（纯读）。支持地点、拍摄时间、文件大小、标签、设备等自然语言条件筛选。
                  ⚠️ 注意：它**无法按相册筛选**，只能按照片自身属性检索；涉及「某个具体相册里的照片」时请改用 searchPhotosByAlbum。
                - searchPhotosByAlbum：按相册查询照片（纯读）。传入相册名称或相册ID，返回该相册内的所有照片及其 fileId。
                  用于「某相册里有哪些照片」「XX 相册里的照片」类需求；只有它能按相册维度检索。
                - queryAlbums：相册问数子Agent（纯读）。按相册名称、创建时间、照片数量等任意维度查询相册自身信息，
                  也能只统计数量（如「我有多少个相册」）。用于「某天创建的相册」「照片数超过10张的相册」
                  「名字带旅行的相册」类需求。注意与 searchPhotosByAlbum 的分工：
                  查「相册的属性」用 queryAlbums，查「相册里的照片」用 searchPhotosByAlbum。
                - getPhotosExif：查询照片的EXIF拍摄信息（纯读）。返回拍摄时间、地点、设备、尺寸、ISO、光圈、快门、
                  焦距、白平衡等详情。用于「这两张照片的EXIF」「用什么设备拍的」「查看拍摄参数」类需求；
                  照片来自用户选中的照片或最近查询结果时 fileIdsJson 传[]即可自动使用。
                - downloadPhotos：为照片生成下载卡片展示给用户（纯读）。用户点击卡片上的按钮即可把照片保存到本地。
                  用于「下载这些照片」「保存到本地」「导出照片」类需求；
                  照片来自用户选中的照片或最近查询结果时 fileIdsJson 传[]即可自动使用。
                - listAlbums：查询用户相册列表（纯读）。
                - createAlbum：创建相册并可放入照片（写操作，生成确认卡片，用户确认后才真正执行）。
                - addPicturesToAlbum：向已有相册添加照片（写操作，同上）。
                - updateAlbumInfo：修改相册名称/描述（写操作，同上；不修改的参数传空字符串）。
                - deleteAlbum：删除相册（写操作，同上；deletePicturesToo=true 时照片一并进回收站，必须向用户明示后果）。
                - removePicturesFromAlbum：从相册移除照片，仅解除关联不删照片（写操作，同上）。
                - generateHtmlPage：网页生成子Agent（纯读），根据传入的照片生成 HTML 展示页。
                  必须传入 fileIdsJson（照片fileId的JSON数组，来自 searchPhotos 的真实结果）；
                  只有当用户明确说"用我选中的照片"时才传[]。

                ## 编排原则
                1. 复杂任务拆解为多步工具调用，按逻辑顺序执行：先读后写、先查数据，再用真实数据执行依赖它的写操作。
                2. 用户要求「查询照片并创建相册/加入相册」时：必须先调用 searchPhotos（按地点/时间/标签等属性）或
                   searchPhotosByAlbum（按指定相册），工具会返回一个照片集引用（形如 photos#1）；
                   把该引用原样填入写操作工具的 photoFileIdsJson 参数，严禁编造 fileId 或 photos#n。
                3. 所有写操作（createAlbum/addPicturesToAlbum/updateAlbumInfo/deleteAlbum/removePicturesFromAlbum）
                   都是「草稿 + 确认」模式：调用成功只代表生成了待确认卡片，必须在最终答复中明确提醒用户在界面上确认。
                   【关键】写操作不会中断你的执行：若后续还有<b>不依赖相册创建结果</b>的步骤
                   （典型如「根据这些照片生成网页」——网页只依赖照片，不依赖相册是否建好），
                   必须在本轮继续调用相应工具完成，不要在生成卡片后就停下等确认。
                   只有确实需要相册的真实 ID 才能继续时，才停下来等用户确认。
                4. 相册名称、描述等参数必须忠实来自用户输入：用户指定了描述就原样传入，不要改写、不要遗漏。
                5. 用户要求修改相册但未说明改哪项时，先用 listAlbums 或结合上下文确定目标相册；
                   updateAlbumInfo 只传用户要求修改的字段，其余传空字符串保持原值。
                6. deleteAlbum 是危险操作：用户未明确要求删除时绝不调用；用户未说明是否连带删照片时，
                   须先向用户确认或保守地传 false（仅删相册不删照片）。
                7. 生成网页时必须把照片 fileId 列表传入 generateHtmlPage 的 fileIdsJson 参数；
                   **绝不能把 fileId 写进需求文本当作图片地址**——网页里的图片地址由工具内部按 fileId 查出。
                8. searchPhotos 结果为空时如实告知用户，不要继续执行依赖照片结果的写操作。
                   用户问照片的 EXIF/拍摄参数（ISO、光圈、快门、设备等）时用 getPhotosExif，不要用 searchPhotos 反复试错。
                   用户要下载/保存/导出照片时用 downloadPhotos，界面会展示下载按钮供用户点击。
                9. 【避免重复查询】若本轮已调用过 searchPhotos 或 searchPhotosByAlbum，或系统提示中已给出「用户选中的照片」
                   /「最近查询到的照片」ID 列表，用户再说「用这些照片/把它们/生成网页/加入相册」时，
                   直接把这些 ID 传入工具（或传 [] 让系统自动使用），**不要调用 searchPhotos / searchPhotosByAlbum 去"找"它们**。
                   只有用户提出全新的筛选条件（如换城市、换时间、换相册）时才重新查询。
                8. 同一写操作不要重复发起；用户未要求写操作时绝不调用写工具。
                9. 工具返回「工具执行失败」时，阅读失败原因，修正参数重试；连续失败两次则向用户说明情况。
                10. 【对用户的表达】最终答复中**不要出现 fileId、ID、UUID 等内部标识**，
                    用自然语言描述照片（如"这 16 张南京照片"）。fileId 只在工具参数内部使用。
                11. 全程使用简体中文。最终答复简洁总结：做了什么、结果如何、下一步（如需用户确认）。
                12. 与照片/相册无关的日常闲聊直接回答，不调用工具。

                ## 照片引用规则（重要）
                - 查询类工具（searchPhotos / searchPhotosByAlbum）返回的是照片集引用，形如 photos#1、photos#2，
                  不再是 fileId 列表。
                - 后续工具要用这批照片时，把引用字符串原样填入 fileIdsJson / photoFileIdsJson（如 "photos#1"）。
                - 传 [] 表示「就用最近一次查出的那批」，系统会自动指向它；不要为了拿 ID 而重复查询。
                - 严禁自己编造 photos#n 或 fileId。引用不存在时工具会报错并列出当前可用的引用，按提示改正后重试。
                """;
    }

    /**
     * 将记忆装配层生成的历史文本（格式固定为「用户: ...」「助手: ...」逐条换行）
     * 还原为消息序列，使多轮对话具备真实上下文结构。
     */
    private List<ChatMessage> parseHistory(String history) {
        List<ChatMessage> result = new ArrayList<>();
        if (history == null || history.isBlank()) {
            return result;
        }
        String role = null;
        StringBuilder buffer = null;
        for (String line : history.split("\n")) {
            if (line.startsWith("用户: ")) {
                flushHistoryMessage(result, role, buffer);
                role = "user";
                buffer = new StringBuilder(line.substring("用户: ".length()));
            } else if (line.startsWith("助手: ")) {
                flushHistoryMessage(result, role, buffer);
                role = "ai";
                buffer = new StringBuilder(line.substring("助手: ".length()));
            } else if (buffer != null) {
                buffer.append("\n").append(line);
            }
        }
        flushHistoryMessage(result, role, buffer);
        return result;
    }

    private void flushHistoryMessage(List<ChatMessage> out, String role, StringBuilder buffer) {
        if (buffer == null || buffer.toString().isBlank()) {
            return;
        }
        if ("user".equals(role)) {
            out.add(UserMessage.from(buffer.toString()));
        } else if ("ai".equals(role)) {
            out.add(AiMessage.from(buffer.toString()));
        }
    }

    /**
     * 推送步骤状态事件（前端据此渲染执行步骤条）。
     *
     * @param id     步骤序号（同一 id 的多次推送表示状态迁移）
     * @param status running / done / failed / waiting_confirm
     * @param title  步骤标题（仅在首次推送时给，后续更新无需重复）
     * @param detail 结果摘要（完成时回填，如「找到 16 张照片」）
     */
    private void pushStep(Consumer<Map<String, Object>> sink, int id, String status,
                          String title, String detail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("status", status);
        if (title != null && !title.isBlank()) {
            data.put("title", title);
        }
        if (detail != null && !detail.isBlank()) {
            data.put("detail", detail);
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "step");
        event.put("data", data);
        sink.accept(event);
    }

    private void pushText(Consumer<Map<String, Object>> sink, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Map<String, Object> msg = new LinkedHashMap<>();
        msg.put("type", "text");
        msg.put("data", text);
        sink.accept(msg);
    }
}
