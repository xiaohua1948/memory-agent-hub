package com.memory.xiaohua.common.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两级缓存管理器 —— 按缓存名懒创建并复用 {@link MultiLevelCache} 实例。
 *
 * <p>L2 使用 {@link StringRedisTemplate} + 项目统一的 {@link ObjectMapper} 手工 JSON 序列化，
 * 而不是复用 {@code RedisConfig} 里的 {@code RedisTemplate<String,Object>}：
 * <ul>
 *   <li>{@code GenericJackson2JsonRedisSerializer} 未注册 JavaTimeModule，
 *       序列化 {@code LocalDateTime} 字段（AiSession/AiMessage 都有）会直接抛异常；</li>
 *   <li>其序列化结果带 {@code @class}，与 PendingActionService 的 Lua 脚本
 *       存在隐式耦合，不宜改动。</li>
 * </ul>
 * 本项目注入的 {@code ObjectMapper} 由 Spring Boot 自动配置，已注册 JavaTimeModule，
 * 可安全处理时间字段。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class MultiLevelCacheManager {

    /** AI 会话（归属校验用，每次对话/查消息都会命中） */
    public static final String CACHE_AI_SESSION = "ai-session";
    /** 会话最近一次查询出的照片 ID（每次对话都要读，回源是整会话消息扫描） */
    public static final String CACHE_AI_RECENT_PHOTOS = "ai-recent-photos";
    /** 记忆回灌数据源（Redis 记忆为空时的 DB 最近 N 条查询） */
    public static final String CACHE_AI_MEMORY = "ai-memory";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AiProperties properties;
    private final Map<String, MultiLevelCache> caches = new ConcurrentHashMap<>();

    public MultiLevelCacheManager(StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  AiProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /** 按名称获取（懒创建，进程内单例） */
    public MultiLevelCache cache(String name) {
        return caches.computeIfAbsent(name, this::create);
    }

    public MultiLevelCache sessionCache() {
        return cache(CACHE_AI_SESSION);
    }

    public MultiLevelCache recentPhotosCache() {
        return cache(CACHE_AI_RECENT_PHOTOS);
    }

    public MultiLevelCache memoryCache() {
        return cache(CACHE_AI_MEMORY);
    }

    private MultiLevelCache create(String name) {
        AiProperties.Cache cfg = properties.getCache();
        log.debug("[Cache] 初始化两级缓存 name={} l1MaxSize={} l1Ttl={}s l2Ttl={}s enabled={}",
                name, cfg.getL1MaxSize(), cfg.getL1TtlSeconds(), cfg.getL2TtlSeconds(), cfg.isEnabled());
        return new MultiLevelCache(
                name,
                cfg.isEnabled(),
                redis,
                objectMapper,
                cfg.getL1MaxSize(),
                Duration.ofSeconds(cfg.getL1TtlSeconds()),
                Duration.ofSeconds(cfg.getL2TtlSeconds()));
    }
}
