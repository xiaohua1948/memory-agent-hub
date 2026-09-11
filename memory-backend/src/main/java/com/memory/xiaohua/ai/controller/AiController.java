package com.memory.xiaohua.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.memory.xiaohua.ai.config.AiChatExecutorConfig;
import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.service.AiService;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.dto.ai.AiChatRequest;
import com.memory.xiaohua.model.dto.ai.AiCreateSessionRequest;
import com.memory.xiaohua.model.dto.ai.AiDownloadZipRequest;
import com.memory.xiaohua.model.dto.ai.AiExecuteRequest;
import com.memory.xiaohua.model.vo.ai.AiMessageVO;
import com.memory.xiaohua.model.vo.ai.AiSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * AI 智能助手接口
 *
 * <p>接口契约（与前端 ai.ts 对齐）：
 * <ul>
 *   <li>POST /ai/session          创建会话</li>
 *   <li>GET  /ai/session          会话列表</li>
 *   <li>GET  /ai/session/{id}/messages  会话历史</li>
 *   <li>DELETE /ai/session/{id}   删除会话</li>
 *   <li>POST /ai/chat             SSE 流式聊天</li>
 *   <li>POST /ai/execute          执行确认的写操作</li>
 * </ul>
 *
 * <p><b>并发模型</b>：SSE 对话由受控线程池 {@code aiChatExecutor} 执行，
 * 取代原先的 {@code new Thread(...)}。线程池 + 有界队列提供了并发上限与背压，
 * 队列满时立刻回一个 error 事件，避免请求无界堆积打爆 JVM 与 LLM 配额。
 *
 * @author xiaohua
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final AiProperties properties;
    private final ThreadPoolTaskExecutor aiChatExecutor;

    public AiController(AiService aiService,
                        AiProperties properties,
                        @Qualifier(AiChatExecutorConfig.AI_CHAT_EXECUTOR) ThreadPoolTaskExecutor aiChatExecutor) {
        this.aiService = aiService;
        this.properties = properties;
        this.aiChatExecutor = aiChatExecutor;
    }

    @PostMapping("/session")
    @Operation(summary = "创建 AI 会话", description = "创建 AI 智能助手会话")
    public BaseResponse<AiSessionVO> createSession(@RequestBody(required = false) AiCreateSessionRequest request) {
        Long userId = currentUserId();
        return ResultUtil.success(aiService.createSession(userId, request), "创建成功");
    }

    @GetMapping("/session")
    @Operation(summary = "会话列表", description = "查询当前用户的 AI 会话列表")
    public BaseResponse<List<AiSessionVO>> listSessions() {
        Long userId = currentUserId();
        return ResultUtil.success(aiService.listSessions(userId), "获取成功");
    }

    @GetMapping("/session/{sessionId}/messages")
    @Operation(summary = "会话历史消息", description = "查询指定会话的历史消息")
    public BaseResponse<List<AiMessageVO>> listMessages(@PathVariable String sessionId) {
        Long userId = currentUserId();
        return ResultUtil.success(aiService.listMessages(userId, sessionId), "获取成功");
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除会话", description = "软删指定会话及其消息，并清理运行时记忆")
    public BaseResponse<?> deleteSession(@PathVariable String sessionId) {
        Long userId = currentUserId();
        aiService.deleteSession(userId, sessionId);
        return ResultUtil.success("删除成功");
    }

    @PostMapping(value = "/download/zip", produces = "application/zip")
    @Operation(summary = "照片打包下载", description = "按 fileId 列表归属校验后打包 ZIP 流式返回")
    public void downloadZip(@RequestBody AiDownloadZipRequest request, HttpServletResponse response) {
        if (request == null || request.getFileIds() == null || request.getFileIds().isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "未指定要下载的照片");
        }
        Long userId = currentUserId();
        aiService.downloadPhotosZip(userId, request.getFileIds(), response);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 聊天（SSE）", description = "流式对话，事件类型 text/card/html/done/error")
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        Long userId = currentUserId();
        SseEmitter emitter = new SseEmitter(0L);

        try {
            // 交给受控线程池：核心/最大线程数与队列容量共同构成并发上限（背压）
            aiChatExecutor.execute(() -> runChat(userId, request, emitter));
        } catch (RejectedExecutionException e) {
            // 线程池与队列均满：明确回一个 error 事件再关闭连接。
            // 不做静默丢弃 —— 否则前端 SSE 会一直等待到超时，用户只看到"卡住"。
            log.warn("[AI] 对话线程池已饱和，拒绝请求 userId={} activeCount={}",
                    userId, aiChatExecutor.getActiveCount());
            sendErrorAndComplete(emitter, "当前对话请求过多，请稍后再试");
        }

        return emitter;
    }

    @PostMapping("/execute")
    @Operation(summary = "执行确认的写操作", description = "用户确认后执行 human-in-the-loop 挂起的写操作")
    public BaseResponse<Map<String, Object>> execute(@RequestBody AiExecuteRequest request) {
        if (request == null || request.getActionId() == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误");
        }
        Long userId = currentUserId();
        return ResultUtil.success(aiService.execute(userId, request.getActionId()), "执行成功");
    }

    /** SSE 对话执行体（运行在线程池线程中） */
    private void runChat(Long userId, AiChatRequest request, SseEmitter emitter) {
        try {
            aiService.chat(userId, request, event -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(String.valueOf(event.getOrDefault("type", "text")))
                            .data(event.getOrDefault("data", "")));
                } catch (IOException e) {
                    log.warn("[AI] SSE 发送失败: {}", e.getMessage());
                }
            });
            emitter.send(SseEmitter.event().name("done").data("done"));
            emitter.complete();
        } catch (Exception e) {
            log.error("[AI] 聊天异常", e);
            sendErrorAndComplete(emitter, e.getMessage());
        }
    }

    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message == null ? "服务异常" : message));
        } catch (IOException ex) {
            // 客户端可能已断开
            log.debug("[AI] error 事件发送失败（客户端可能已断开）: {}", ex.getMessage());
        }
        emitter.complete();
    }

    private Long currentUserId() {
        Object loginId = StpUtil.getLoginId();
        if (loginId == null) {
            throw new BusinessException(StatusCode.NOT_LOGIN_ERROR);
        }
        return Long.parseLong(loginId.toString());
    }
}
