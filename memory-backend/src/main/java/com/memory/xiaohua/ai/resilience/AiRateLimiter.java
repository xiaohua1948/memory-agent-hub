package com.memory.xiaohua.ai.resilience;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 令牌桶限流器（Redisson {@link RRateLimiter}）。
 *
 * <p>限流维度是「场景 + 用户」：{@code ai:rate:{scene}:{userId}}。
 * 按用户而非全局限流的原因 —— 单个用户狂刷对话会同时消耗 LLM 额度、
 * 对话线程池与 Redis 带宽，而全局限流会让一个用户拖垮所有人；
 * 按用户维度可以做到「谁刷谁受限，他人不受影响」。
 *
 * <p>多实例一致性：令牌桶状态存在 Redis，多个后端实例共享同一个桶 ——
 * 这是用 Redisson 而非本地 {@code Semaphore} 的核心原因，
 * 本地限流在水平扩容后实际配额会变成「配额 × 实例数」。
 *
 * <p>降级策略：限流开关关闭、Redisson 未装配（Redis 不可达）、
 * 或限流检查过程抛异常时，一律**放行**并记录告警。
 * 限流是过载保护手段，其自身故障不应升级为业务不可用。
 *
 * @author xiaohua
 */
@Slf4j
@Component
public class AiRateLimiter {

    private static final String KEY_PREFIX = "ai:rate:";

    /** 场景：对话（SSE 流式） */
    public static final String SCENE_CHAT = "chat";

    private final AiProperties properties;

    /** Redisson 未装配（ai.enabled=false 或 Redis 不可达）时为 null → 降级放行 */
    @Autowired(required = false)
    private RedissonClient redissonClient;

    public AiRateLimiter(AiProperties properties) {
        this.properties = properties;
    }

    /**
     * 尝试获取一个令牌，失败则抛出业务异常。
     *
     * @param userId 用户 ID（匿名时为 null）
     * @param scene  场景标识，便于不同接口配置独立配额
     * @throws BusinessException 触发限流时抛出（错误码 {@link StatusCode#AI_RATE_LIMITED}）
     */
    public void acquireOrThrow(Long userId, String scene) {
        AiProperties.RateLimit cfg = properties.getRateLimit();
        if (!cfg.isEnabled() || redissonClient == null) {
            return;
        }

        String key = KEY_PREFIX + scene + ":" + (userId == null ? "anonymous" : userId);
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(key);
            // trySetRate 幂等：仅在桶尚未初始化时生效，多实例并发/重启都不会重置已消耗的令牌
            limiter.trySetRate(RateType.OVERALL, cfg.getPermits(), cfg.getIntervalSeconds(),
                    RateIntervalUnit.SECONDS);
            if (!limiter.tryAcquire(1)) {
                log.warn("[AI][RateLimit] 触发限流 scene={} userId={} 配额={}/{}s",
                        scene, userId, cfg.getPermits(), cfg.getIntervalSeconds());
                throw new BusinessException(StatusCode.AI_RATE_LIMITED,
                        "请求过于频繁，请稍后再试（限额 " + cfg.getPermits() + " 次/"
                                + cfg.getIntervalSeconds() + " 秒）");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 抖断 / 序列化异常等：限流不可用不应阻断对话
            log.warn("[AI][RateLimit] 限流检查异常，降级放行 scene={} userId={}: {}",
                    scene, userId, e.getMessage());
        }
    }
}
