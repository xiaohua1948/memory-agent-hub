package com.memory.xiaohua.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.ai.service.AiService;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.dto.ai.AiChatRequest;
import com.memory.xiaohua.model.dto.ai.AiCreateSessionRequest;
import com.memory.xiaohua.model.dto.ai.AiExecuteRequest;
import com.memory.xiaohua.model.vo.ai.AiMessageVO;
import com.memory.xiaohua.model.vo.ai.AiSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
 * @author xiaohua
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final AiProperties properties;

    public AiController(AiService aiService, AiProperties properties) {
        this.aiService = aiService;
        this.properties = properties;
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

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 聊天（SSE）", description = "流式对话，事件类型 text/card/html/done/error")
    public SseEmitter chat(@RequestBody AiChatRequest request) {
        Long userId = currentUserId();
        SseEmitter emitter = new SseEmitter(0L);

        // 异步执行，避免阻塞 SSE 连接线程
        new Thread(() -> {
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
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, "ai-chat-thread").start();

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

    private Long currentUserId() {
        Object loginId = StpUtil.getLoginId();
        if (loginId == null) {
            throw new BusinessException(StatusCode.NOT_LOGIN_ERROR);
        }
        return Long.parseLong(loginId.toString());
    }
}
