package com.memory.xiaohua.ai.action;

import com.memory.xiaohua.ai.config.AiProperties;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.mapper.AiPendingActionMapper;
import com.memory.xiaohua.model.entity.AiPendingAction;
import com.memory.xiaohua.model.vo.ai.PendingActionVO;
import com.memory.xiaohua.utils.auth.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Human-in-the-loop 待确认写操作服务
 *
 * <p>核心安全设计：Agent 的写操作**只出草稿不落库**，本服务负责：
 * <ul>
 *   <li>生成 actionId（UUID），写 Redis（TTL 自动过期）+ DB（兜底审计）双存储；</li>
 *   <li>用户确认时用 Redis Lua 脚本原子校验 + 状态机迁移（PENDING→CONFIRMED），
 *       保证「同一 actionId 只执行一次」，杜绝重复点击重复写库；</li>
 *   <li>校验 userId 归属，防止越权确认他人操作。</li>
 * </ul>
 *
 * @author xiaohua
 */
@Slf4j
@Service
public class PendingActionService {

    private static final String KEY_PREFIX = "ai:pending:";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    /**
     * Lua：仅当 key 存在且 userId 匹配且状态为 PENDING 时，才置为 CONFIRMED（原子幂等）。
     *
     * <p>兼容性说明：本工程 RedisConfig 的 hashValue 用 GenericJackson2JsonRedisSerializer，
     * 字符串字段落库时会序列化为带双引号的 JSON 字符串（如 "123"），因此 Lua 侧读取比较前
     * 需 strip 掉首尾双引号；写入时同样以 JSON 字符串字面量（带引号）写回，保证 Java 侧
     * GenericJackson 能正常反序列化。
     */
    private static final String LUA_CONFIRM =
            "local key = KEYS[1]; local uid = ARGV[1]; " +
            "local function strip(s) " +
            "  if type(s) == 'string' and #s >= 2 and string.sub(s,1,1) == '\"' and string.sub(s,-1) == '\"' then " +
            "    return string.sub(s, 2, -2) " +
            "  end " +
            "  return s " +
            "end " +
            "local v = redis.call('HGET', key, 'userId'); " +
            "local s = redis.call('HGET', key, 'status'); " +
            // userId 由 GenericJackson 序列化后（带引号）作为 ARGV 传入，需与 HGET 的 userId 一并 strip 后再比较
            "if not v or strip(v) ~= strip(uid) then return 0; end; " +
            "if strip(s) ~= 'PENDING' then return 0; end; " +
            "redis.call('HSET', key, 'status', '\"CONFIRMED\"'); " +
            "return 1;";

    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiPendingActionMapper pendingActionMapper;
    private final AiProperties properties;

    public PendingActionService(RedisUtil redisUtil,
                                RedisTemplate<String, Object> redisTemplate,
                                AiPendingActionMapper pendingActionMapper,
                                AiProperties properties) {
        this.redisUtil = redisUtil;
        this.redisTemplate = redisTemplate;
        this.pendingActionMapper = pendingActionMapper;
        this.properties = properties;
    }

    /**
     * 创建一个待确认操作（草稿，不落库执行）。
     *
     * @param userId     用户
     * @param sessionId  会话
     * @param actionType 操作类型
     * @param execParams 执行参数 JSON
     * @param vo         卡片展示信息
     * @return actionId
     */
    public String createPendingAction(Long userId, String sessionId, String actionType,
                                      String execParams, PendingActionVO vo) {
        String actionId = UUID.randomUUID().toString().replace("-", "");
        int ttl = properties.getPendingActionTtlSeconds();

        // 1. Redis 高速存储（含卡片展示信息）
        String key = KEY_PREFIX + actionId;
        redisUtil.hset(key, "userId", String.valueOf(userId));
        redisUtil.hset(key, "sessionId", sessionId);
        redisUtil.hset(key, "actionType", actionType);
        redisUtil.hset(key, "execParams", execParams);
        redisUtil.hset(key, "status", STATUS_PENDING);
        redisUtil.expire(key, ttl, TimeUnit.SECONDS);

        // 2. DB 兜底审计
        AiPendingAction action = new AiPendingAction();
        action.setActionId(actionId);
        action.setSessionId(sessionId);
        action.setUserId(userId);
        action.setActionType(actionType);
        action.setExecParams(execParams);
        action.setStatus(STATUS_PENDING);
        action.setCreateTime(LocalDateTime.now());
        pendingActionMapper.insert(action);

        log.info("[AI] 创建待确认操作 actionId={}, type={}, userId={}", actionId, actionType, userId);
        return actionId;
    }

    /**
     * 用户确认：Redis Lua 原子校验 + 状态迁移。
     *
     * @param actionId 操作ID
     * @param userId   用户
     * @return 执行参数 JSON（校验失败抛异常）
     */
    public String confirm(String actionId, Long userId) {
        String key = KEY_PREFIX + actionId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_CONFIRM, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(userId));

        if (result == null || result == 0L) {
            // 可能已过期 / 已确认 / 归属不符
            String status = String.valueOf(redisUtil.hget(key, "status"));
            if ("CONFIRMED".equals(status)) {
                throw new BusinessException(StatusCode.OPERATION_ERROR, "该操作已执行，请勿重复确认");
            }
            throw new BusinessException(StatusCode.OPERATION_ERROR, "操作不存在、已过期或无权限确认");
        }

        // Redis 确认成功后同步 DB 状态（乐观状态迁移，防并发）
        pendingActionMapper.updateStatus(actionId, userId, STATUS_PENDING, STATUS_CONFIRMED);

        Object params = redisUtil.hget(key, "execParams");
        String execParams = params == null ? null : String.valueOf(params);
        if (execParams == null || execParams.isBlank()) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "执行参数缺失");
        }
        return execParams;
    }

    /** 取消待确认操作（用户点取消） */
    public void cancel(String actionId, Long userId) {
        String key = KEY_PREFIX + actionId;
        Object owner = redisUtil.hget(key, "userId");
        if (owner == null || !String.valueOf(owner).equals(String.valueOf(userId))) {
            throw new BusinessException(StatusCode.OPERATION_ERROR, "无权限取消该操作");
        }
        redisUtil.hset(key, "status", STATUS_CANCELLED);
        pendingActionMapper.updateStatus(actionId, userId, STATUS_PENDING, STATUS_CANCELLED);
    }
}
