package com.memory.xiaohua.ai.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 对话记忆 Redis 存储配置
 *
 * <p>对齐《AI智能助手-优化计划.md》§4.3：手动构建 {@link RedisChatMemoryStore}，
 * 复用业务 Redis 连接信息（spring.data.redis.*）。
 *
 * <p>⚠️ 已核对 langchain4j-community-redis {@code 1.1.0-beta7} 的真实 API：
 * <ul>
 *   <li>builder 方法：{@code host(String) / port(Integer) / user(String) / password(String) / ttl(Long) / prefix(String)}；</li>
 *   <li>{@code ttl} 单位为 <b>秒</b>（内部走 Jedis {@code setex(key, seconds, value)}）；</li>
 *   <li>默认 {@code prefix} 为空字符串，故显式设 {@code ai-chat-memory} 前缀，
 *       避免与 sa-token / PendingAction 等业务 key 冲突。</li>
 * </ul>
 *
 * <p>⚠️ 坑（1.1.0-beta7 反编译实测）：{@code RedisChatMemoryStore} 构造时
 * <b>仅当 {@code user != null} 才走 {@code JedisPooled(host, port, user, password)} 认证分支</b>，
 * 否则走 {@code JedisPooled(host, port)} —— 完全不带 AUTH。
 * 因此只调 {@code .password(pwd)} 而不设 {@code user}，连接仍然无认证，
 * 对开启了 {@code requirepass} 的 Redis 会在首次读写时抛
 * {@code JedisAccessControlException: NOAUTH Authentication required.}
 * 必须 {@code user("default")} 与 {@code password(pwd)} 成对设置（Redis 默认 ACL 用户即 default，
 * 其密码由 requirepass 指定）。
 *
 * <p>仅当 {@code ai.enabled=true} 时注册；未启用时 {@code AiChatMemoryFactory} 降级为进程内内存。
 *
 * @author xiaohua
 */
@Configuration
public class AiChatMemoryConfig {

    private static final String KEY_PREFIX = "ai-chat-memory";

    @Bean
    @ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
    public ChatMemoryStore chatMemoryStore(RedisProperties redisProperties, AiProperties properties) {
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(redisProperties.getHost())
                .port(redisProperties.getPort())
                .prefix(KEY_PREFIX)
                .ttl((long) properties.getMemoryTtlSeconds());

        // user 与 password 必须成对设置：只设 password 会被 store 忽略（见类注释），
        // 导致 Jedis 连接无认证，读写时抛 NOAUTH。Redis 默认 ACL 用户为 default。
        String password = redisProperties.getPassword();
        if (password != null && !password.isBlank()) {
            builder.user("default").password(password);
        }

        return builder.build();
    }
}
