package com.memory.xiaohua.ai.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 工具执行上下文 —— 通过 ThreadLocal 向子 Agent 工具注入请求级信息
 *
 * <p>设计动机：暴露给大模型的工具方法签名<strong>不含 userId/sessionId 等身份参数</strong>，
 * 防止模型伪造身份、越权或漏填必填参数。主 Agent（{@link MainAgent}）在编排循环前绑定
 * 上下文，工具内部通过 {@link #current()} 获取；循环结束必须 {@link #clear()}，
 * 避免线程复用导致串号 / 内存泄漏。
 *
 * <p>线程模型：AI 对话在独立线程中同步完成整个 tool-use 循环
 * （AiController 的 ai-chat-thread），ThreadLocal 在该线程内安全。
 *
 * @author xiaohua
 */
public final class AgentToolContext {

    private static final ThreadLocal<AgentToolContext> HOLDER = new ThreadLocal<>();

    /** 当前用户（由编排层从登录态注入，工具内只读） */
    private final Long userId;

    /** 当前会话 */
    private final String sessionId;

    /** 用户选中的照片上下文（网页生成子 Agent 使用） */
    private final List<String> contextFileIds;

    /** 本会话最近一次查询出的照片（多轮上下文复用，模型无需重复查询） */
    private final List<String> recentPhotoIds;

    /** 本轮工具已查出的照片（供同轮后续工具复用，如查询后接着建相册） */
    private final java.util.Set<String> queriedPhotoIds =
            java.util.Collections.synchronizedSet(new java.util.LinkedHashSet<>());

    /** SSE 事件回调：工具执行过程中的进度 / 产物（photos / card / html）实时推送 */
    private final Consumer<Map<String, Object>> sink;

    private AgentToolContext(Long userId, String sessionId, List<String> contextFileIds,
                             List<String> recentPhotoIds,
                             Consumer<Map<String, Object>> sink) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.contextFileIds = contextFileIds;
        this.recentPhotoIds = recentPhotoIds;
        this.sink = sink;
    }

    /** 编排循环开始前绑定（调用方保证 finally 中 clear） */
    public static void bind(Long userId, String sessionId, List<String> contextFileIds,
                            List<String> recentPhotoIds, Consumer<Map<String, Object>> sink) {
        HOLDER.set(new AgentToolContext(userId, sessionId, contextFileIds, recentPhotoIds, sink));
    }

    /** 记录本轮已查询出的照片，供同轮后续工具复用 */
    public void addQueriedPhotoIds(List<String> ids) {
        if (ids != null) {
            queriedPhotoIds.addAll(ids);
        }
    }

    public List<String> getQueriedPhotoIds() {
        return List.copyOf(queriedPhotoIds);
    }

    /** 获取当前上下文（未绑定即调用视为编程错误） */
    public static AgentToolContext current() {
        AgentToolContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException("工具执行上下文未绑定，仅允许在主 Agent 编排线程内调用工具");
        }
        return ctx;
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 推送 SSE 事件（type + data），如 photos / card / html */
    public void pushEvent(String type, Object data) {
        if (sink == null) {
            return;
        }
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("data", data);
        sink.accept(event);
    }

    public Long getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public List<String> getContextFileIds() {
        return contextFileIds;
    }

    public List<String> getRecentPhotoIds() {
        return recentPhotoIds;
    }
}
