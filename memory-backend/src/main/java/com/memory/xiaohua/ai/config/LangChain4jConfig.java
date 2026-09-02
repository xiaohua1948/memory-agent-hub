package com.memory.xiaohua.ai.config;

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
        return OpenAiChatModel.builder()
                .apiKey(resolveKey(m))
                .baseUrl(properties.getBaseUrl())
                .modelName(defaultIfBlank(m.getModelName(), "deepseek-chat"))
                .temperature(m.getTemperature() == null ? 0.0 : m.getTemperature())
                .maxTokens(m.getMaxTokens() == null ? 100 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(30))
                .build();
    }

    /** 主任务模型 */
    @Bean(name = "mainModel")
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatModel mainModel() {
        AiProperties.Model m = properties.getMain();
        return OpenAiChatModel.builder()
                .apiKey(resolveKey(m))
                .baseUrl(properties.getBaseUrl())
                .modelName(defaultIfBlank(m.getModelName(), "deepseek-chat"))
                .temperature(m.getTemperature() == null ? 0.1 : m.getTemperature())
                .maxTokens(m.getMaxTokens() == null ? 2048 : m.getMaxTokens())
                .timeout(Duration.ofSeconds(120))
                .build();
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
