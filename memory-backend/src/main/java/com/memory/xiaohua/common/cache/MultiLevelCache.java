package com.memory.xiaohua.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 两级缓存：L1 Caffeine（进程内，微秒级）+ L2 Redis（跨实例共享）。
 *
 * <p><b>读取顺序</b>：L1 → L2（命中回填 L1）→ loader（回填 L2 + L1）。
 * <p><b>写入顺序</b>：先 L2 后 L1 —— 若 L2 写失败仅降级为本地缓存，
 * 不会出现「L1 有值但 L2 没有」导致其他实例长期读不到新值的假命中。
 *
 * <p><b>一致性取舍</b>：L1 无法被其他实例主动失效，因此 L1 TTL 必须显著短于 L2，
 * 把跨实例脏读窗口限制在秒级（默认 5s）。本实例的写路径需主动调用
 * {@link #evict(String)}；其他实例依赖 L1 短 TTL 自然收敛。
 *
 * <p><b>空值策略</b>：null 不缓存（避免把「暂时不存在」固化成 5 分钟脏读），
 * 但空集合会缓存 —— 空结果恰恰是最需要缓存来防击穿的高频场景。
 *
 * @author xiaohua
 */
@Slf4j
public class MultiLevelCache {

    private final String name;
    private final String keyPrefix;
    private final boolean enabled;
    private final Cache<String, Object> l1;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration l2Ttl;

    public MultiLevelCache(String name,
                           boolean enabled,
                           StringRedisTemplate redis,
                           ObjectMapper objectMapper,
                           long l1MaxSize,
                           Duration l1Ttl,
                           Duration l2Ttl) {
        this.name = name;
        this.enabled = enabled;
        this.keyPrefix = "cache:" + name + ":";
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.l2Ttl = l2Ttl;
        this.l1 = Caffeine.newBuilder()
                .maximumSize(l1MaxSize)
                .expireAfterWrite(l1Ttl)
                .recordStats()
                .build();
    }

    /**
     * 读缓存，未命中则回源并写回两级缓存。
     *
     * @param key    业务键（内部自动加缓存名前缀）
     * @param type   反序列化目标类型（L2 为 JSON 字符串，需显式类型信息）
     * @param loader 回源逻辑
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, TypeReference<T> type, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }
        String cacheKey = keyPrefix + key;
        Object hit = l1.getIfPresent(cacheKey);
        if (hit != null) {
            return (T) hit;
        }
        T fromRedis = readRedis(cacheKey, type);
        if (fromRedis != null) {
            l1.put(cacheKey, fromRedis);
            return fromRedis;
        }
        T loaded = loader.get();
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }

    /** 只读缓存（不回源），用于「有就用、没有就算了」的可选加速场景 */
    @SuppressWarnings("unchecked")
    public <T> T getIfPresent(String key, TypeReference<T> type) {
        if (!enabled) {
            return null;
        }
        String cacheKey = keyPrefix + key;
        Object hit = l1.getIfPresent(cacheKey);
        if (hit != null) {
            return (T) hit;
        }
        T fromRedis = readRedis(cacheKey, type);
        if (fromRedis != null) {
            l1.put(cacheKey, fromRedis);
        }
        return fromRedis;
    }

    /** 写入两级缓存（先 L2 后 L1） */
    public void put(String key, Object value) {
        if (!enabled || value == null) {
            return;
        }
        String cacheKey = keyPrefix + key;
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(cacheKey, json, l2Ttl);
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 写入失败，降级为仅本地缓存 key={}: {}", name, key, e.getMessage());
        }
        l1.put(cacheKey, value);
    }

    /** 失效单个键（写路径必须调用，否则本实例会读到旧值直到 L1 过期） */
    public void evict(String key) {
        if (!enabled) {
            return;
        }
        String cacheKey = keyPrefix + key;
        l1.invalidate(cacheKey);
        try {
            redis.delete(cacheKey);
        } catch (Exception e) {
            log.warn("[Cache:{}] L2 删除失败 key={}: {}", name, key, e.getMessage());
        }
    }

    /** 批量失效 */
    public void evictAll(Collection<String> keys) {
        if (!enabled || keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            evict(key);
        }
    }

    /** L1 命中率等统计（排查缓存是否真的生效） */
    public String l1Stats() {
        return l1.stats().toString();
    }

    private <T> T readRedis(String cacheKey, TypeReference<T> type) {
        String json;
        try {
            json = redis.opsForValue().get(cacheKey);
        } catch (Exception e) {
            // Redis 故障不应让业务失败：当作未命中，回源查库（DB 是真相源）
            log.warn("[Cache:{}] L2 读取异常，降级回源 key={}: {}", name, cacheKey, e.getMessage());
            return null;
        }
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            // 脏数据 / 类型变更：删掉该键让其回源，避免此后每次读取都反序列化失败
            log.warn("[Cache:{}] L2 反序列化失败，已清除该键 key={}: {}", name, cacheKey, e.getMessage());
            try {
                redis.delete(cacheKey);
            } catch (Exception ignored) {
                // 删除失败无碍，键本身有 TTL
            }
            return null;
        }
    }
}
