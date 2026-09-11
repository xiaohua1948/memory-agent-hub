package com.memory.xiaohua.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置 —— 用于令牌桶限流（{@code RRateLimiter}）。
 *
 * <p>刻意使用核心包 {@code org.redisson:redisson} 手工建 Bean，
 * 而不是 {@code redisson-spring-boot-starter}：starter 会自动配置并**替换**
 * {@code RedisConnectionFactory}，从而影响现有 {@code RedisTemplate}
 * （GenericJackson2JsonRedisSerializer）与 Sa-Token 的 Redis 会话存储。
 * 这里只需要 RRateLimiter，不触碰 Spring Data Redis 的连接层。
 *
 * <p>复用 {@code spring.data.redis.*} 的连接信息，与业务 Redis 同实例不同 key 前缀。
 *
 * <p><b>降级策略</b>：Redis 不可达时 {@code Redisson.create} 会抛异常，
 * 此时返回 null（限流器注入方使用 {@code @Autowired(required = false)}），
 * {@code AiRateLimiter} 检测到 null 后静默放行 —— 限流是保护措施，
 * 不应因为它的依赖故障而让整个 AI 对话不可用。
 *
 * @author xiaohua
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        SingleServerConfig server = config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                // 限流场景连接数需求很低，控制资源占用
                .setConnectionMinimumIdleSize(1)
                .setConnectionPoolSize(16)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setRetryAttempts(1)
                .setRetryInterval(500);

        String password = redisProperties.getPassword();
        if (password != null && !password.isBlank()) {
            server.setPassword(password);
        }

        try {
            RedissonClient client = Redisson.create(config);
            log.info("[AI][Redisson] 客户端初始化完成 address=redis://{}:{} db={}",
                    redisProperties.getHost(), redisProperties.getPort(), redisProperties.getDatabase());
            return client;
        } catch (Exception e) {
            log.error("[AI][Redisson] 初始化失败，令牌桶限流将降级为放行: {}", e.getMessage());
            return null;
        }
    }
}
