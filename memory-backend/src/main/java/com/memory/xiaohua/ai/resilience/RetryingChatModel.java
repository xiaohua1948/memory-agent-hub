package com.memory.xiaohua.ai.resilience;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;

/**
 * 带指数退避重试的 {@link ChatModel} 装饰器。
 *
 * <p><b>为什么只重写 {@link #chat(ChatRequest)}</b>：LangChain4j 的
 * {@code chat(String)}、{@code chat(List&lt;ChatMessage&gt;)} 等便捷重载在接口默认实现里
 * 最终都会收敛到 {@code chat(ChatRequest)}。因此在这一处装饰，就能让**全部调用点**
 * 自动获得重试能力 —— 主 Agent 编排（含 function calling）、text2sql、query planner、
 * 网页生成、标题生成，无需逐处改造，也不会遗漏。
 *
 * <p>重试边界：只对超时 / 429 / 5xx / 连接异常等瞬时故障重试（见 {@link RetryClassifier}），
 * 业务异常直接抛出，避免无意义地重复消耗 token。
 *
 * <p>重试期间会重新发出完全相同的请求（含 tool specifications），
 * 因此对 function calling 是安全的：工具在一次失败的请求中并未被执行。
 *
 * @author xiaohua
 */
@Slf4j
public class RetryingChatModel implements ChatModel {

    private final ChatModel delegate;
    /** 场景标识，仅用于日志定位（router / main / webpage） */
    private final String scene;
    private final int maxAttempts;
    private final long initialBackoffMs;
    private final double multiplier;
    private final long maxBackoffMs;

    public RetryingChatModel(ChatModel delegate,
                             String scene,
                             int maxAttempts,
                             long initialBackoffMs,
                             double multiplier,
                             long maxBackoffMs) {
        this.delegate = delegate;
        this.scene = scene;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMs = Math.max(0, initialBackoffMs);
        this.multiplier = multiplier <= 0 ? 1.0 : multiplier;
        this.maxBackoffMs = Math.max(this.initialBackoffMs, maxBackoffMs);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long backoff = initialBackoffMs;
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return delegate.chat(request);
            } catch (RuntimeException e) {
                lastError = e;
                boolean retryable = RetryClassifier.isRetryable(e);
                if (attempt >= maxAttempts || !retryable) {
                    if (retryable) {
                        log.error("[AI][Retry][{}] 已重试 {} 次仍失败: {}",
                                scene, attempt - 1, RetryClassifier.rootMessage(e));
                    }
                    throw e;
                }
                log.warn("[AI][Retry][{}] 第 {}/{} 次调用失败，{}ms 后重试: {}",
                        scene, attempt, maxAttempts, backoff, RetryClassifier.rootMessage(e));
                sleepQuietly(backoff);
                backoff = Math.min(maxBackoffMs, (long) (backoff * multiplier));
            }
        }
        // 理论上不可达（循环内要么 return 要么 throw），仅为满足编译器的确定性返回
        throw lastError == null ? new IllegalStateException("LLM 调用未产生结果") : lastError;
    }

    // ==================== 以下均委托给被装饰模型，避免丢失能力协商信息 ====================

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return delegate.defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return delegate.listeners();
    }

    @Override
    public ModelProvider provider() {
        return delegate.provider();
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return delegate.supportedCapabilities();
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // 恢复中断标记，交由上层感知；同时放弃重试以免吞掉停机信号
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM 重试等待被中断", e);
        }
    }
}
