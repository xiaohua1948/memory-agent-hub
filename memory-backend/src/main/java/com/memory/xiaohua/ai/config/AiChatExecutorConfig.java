package com.memory.xiaohua.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI 对话执行线程池配置。
 *
 * <p>取代原先在 {@code AiController#chat} 中为每个 SSE 请求裸起 {@code new Thread(...)} 的做法：
 * 裸线程没有并发上限、没有队列、没有背压，高并发下线程数线性增长，
 * 会同时打爆 JVM 线程栈内存与下游 LLM 配额，且没有任何限流信号可观测。
 *
 * <p>线程池关键取舍：
 * <ul>
 *   <li><b>有界队列</b>：容量固定，防止请求无界堆积导致 OOM；</li>
 *   <li><b>AbortPolicy</b>：队列满时立刻抛出 {@code RejectedExecutionException}，
 *       由调用方（Controller）回一个明确的 SSE error 事件。这是刻意选择的 ——
 *       静默丢弃会让前端 SSE 连接一直挂着直到超时，用户体验更差；</li>
 *   <li><b>优雅停机</b>：等待在途对话结束（最多 {@code await-termination-seconds} 秒），
 *       避免重启时正在生成的回答被硬切断。</li>
 * </ul>
 *
 * <p>池大小按「一次对话 = 一个线程 + 若干次 LLM 网络等待」估算：
 * 由于 SSE 全程占用一个线程，池大小实际限制了同时进行的对话数，
 * 需与 {@code ai.rate-limit} 的配额协同配置。
 *
 * @author xiaohua
 */
@Slf4j
@Configuration
public class AiChatExecutorConfig {

    public static final String AI_CHAT_EXECUTOR = "aiChatExecutor";

    @Bean(name = AI_CHAT_EXECUTOR, destroyMethod = "shutdown")
    public ThreadPoolTaskExecutor aiChatExecutor(AiProperties properties) {
        AiProperties.ChatExecutor cfg = properties.getChatExecutor();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(cfg.getCoreSize());
        executor.setMaxPoolSize(cfg.getMaxSize());
        executor.setQueueCapacity(cfg.getQueueCapacity());
        executor.setKeepAliveSeconds(cfg.getKeepAliveSeconds());
        executor.setThreadNamePrefix("ai-chat-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(cfg.getAwaitTerminationSeconds());
        executor.initialize();

        log.info("[AI][Executor] 对话线程池已初始化 core={} max={} queue={}",
                cfg.getCoreSize(), cfg.getMaxSize(), cfg.getQueueCapacity());
        return executor;
    }
}
