package com.memory.xiaohua.ai.config;

import com.memory.xiaohua.ai.resilience.RetryingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j 模型 Bean 配置
 *
 * <p>按角色注册两个 {@link ChatModel}：
 * <ul>
 *   <li>routerModel —— 轻量模型，意图分诊 / 标题生成，低温度 + 低 max-tokens</li>
 *   <li>mainModel   —— 主任务模型，SQL 生成 / 相册编排 / 网页生成</li>
 * </ul>
 *
 * <p>仅当 {@code ai.enabled=true} 时才注册 Bean（{@link ConditionalOnProperty}），
 * 未启用时注入点使用 {@code @Autowired(required = false)}，Service 层统一降级为友好提示，
 * 避免 Bean 缺失导致启动失败。
 *
 * <p><b>统一重试</b>：三个模型都通过 {@link RetryingChatModel} 包装后再暴露。
 * 由于 LangChain4j 的各类 {@code chat(...)} 重载最终都收敛到 {@code chat(ChatRequest)}，
 * 这一层装饰即可让全部调用点（主 Agent 编排、text2sql、query planner、网页生成、
 * 标题生成）获得指数退避重试，无需逐处改造。SDK 自身的重试已关闭（{@code maxRetries(0)}），
 * 避免「SDK 重试 × 外层重试」叠加导致实际调用次数与配置严重不符。
 *
 * @author xiaohua
 */
@Slf4j
@Configuration
public class LangChain4jConfig {

    private final AiProperties properties;

    public LangChain4jConfig(AiProperties properties) {
        this.properties = properties;
    }

    /** 路由模型（轻量） */
    @Bean(name = "routerModel")
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatModel routerModel() {
        AiProperties.Model m = properties.getRouter();
        ChatModel delegate = OpenAiChatModel.builder()
                .apiKey(resolveKey(m))
                .baseUrl(properties.getBaseUrl())
                .modelName(defaultIfBlank(m.getModelName(), "deepseek-chat"))
                .temperature(m.getTemperature() == null ? 0.0 : m.getTemperature())
                .maxTokens(m.getMaxTokens() == null ? 100 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .build();
        return wrap(delegate, "router");
    }

    /** 主任务模型 */
    @Bean(name = "mainModel")
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatModel mainModel() {
        AiProperties.Model m = properties.getMain();
        ChatModel delegate = OpenAiChatModel.builder()
                .apiKey(resolveKey(m))
                .baseUrl(properties.getBaseUrl())
                .modelName(defaultIfBlank(m.getModelName(), "deepseek-chat"))
                .temperature(m.getTemperature() == null ? 0.1 : m.getTemperature())
                .maxTokens(m.getMaxTokens() == null ? 2048 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .maxRetries(0)
                .build();
        return wrap(delegate, "main");
    }

    /**
     * 网页生成模型：与 mainModel 同型号，但 max-tokens 大幅放大（默认 8192）。
     *
     * <p>存在原因：完整 HTML 单页（含内联 CSS）通常需要 6000+ tokens，主任务的 2048 上限
     * 会让输出在 CSS 中途被截断，导致网页结构破损、照片区域根本没生成到。
     */
    @Bean(name = "webpageModel")
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatModel webpageModel() {
        AiProperties.Model m = properties.getWebpage();
        AiProperties.Model main = properties.getMain();
        // 未显式配置时继承主模型配置，仅放大 token 上限
        String modelName = defaultIfBlank(m.getModelName(), defaultIfBlank(main.getModelName(), "deepseek-chat"));
        Integer maxTokens = m.getMaxTokens() != null && m.getMaxTokens() > 4096
                ? m.getMaxTokens() : 8192;
        Double temperature = m.getTemperature() != null
                ? m.getTemperature() : (main.getTemperature() == null ? 0.2 : main.getTemperature());
        ChatModel delegate = OpenAiChatModel.builder()
                .apiKey(resolveKey(m))
                .baseUrl(properties.getBaseUrl())
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(180))
                .maxRetries(0)
                .build();
        return wrap(delegate, "webpage");
    }

    /** 统一包装：注入按配置的指数退避重试能力 */
    private ChatModel wrap(ChatModel delegate, String scene) {
        AiProperties.Retry retry = properties.getRetry();
        return new RetryingChatModel(delegate, scene,
                retry.getMaxAttempts(), retry.getInitialBackoffMs(),
                retry.getMultiplier(), retry.getMaxBackoffMs());
    }

    private String resolveKey(AiProperties.Model m) {
        String key = m.getApiKey();
        if (key == null || key.isBlank()) {
            key = properties.getApiKey();
        }
        return key;
    }

    private String defaultIfBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
