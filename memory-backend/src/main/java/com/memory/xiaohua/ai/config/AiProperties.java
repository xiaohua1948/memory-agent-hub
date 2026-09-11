package com.memory.xiaohua.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 智能助手配置属性
 *
 * <p>通过 application.yml 中 ai.* 前缀配置。支持按「角色」拆分模型：
 * <ul>
 *   <li>router —— 轻量模型（deepseek-chat，max-tokens=100）做意图分诊 / 标题生成</li>
 *   <li>main   —— 主任务模型（deepseek-chat）做 SQL 生成 / 相册编排 / 网页生成</li>
 * </ul>
 *
 * @author xiaohua
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** 是否启用 AI 能力（未配置 API Key 时自动降级为提示） */
    private boolean enabled = false;

    /** 通用 OpenAI 兼容端点（DeepSeek） */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 通用 API Key（所有模型共用，可被具体模型覆盖） */
    private String apiKey = "";

    /** 路由模型配置 */
    private Model router = new Model();

    /** 主任务模型配置 */
    private Model main = new Model();

    /**
     * 网页生成模型配置（默认继承 main 的 key/baseUrl，仅放大 max-tokens）。
     *
     * <p>网页生成是 token 消耗最大的子任务：一个带复杂 CSS 的单页通常需 6000~12000 tokens，
     * 用主任务的 2048 会导致 HTML 在 CSS 中途被截断（表现为网页破损、图片区域根本没生成到）。
     */
    private Model webpage = new Model();

    /** 最大工具调用轮数（有界 tool-use 循环） */
    private int maxToolIterations = 20;

    /** 记忆窗口大小（MessageWindowChatMemory.maxMessages，保留最近 N 条消息） */
    private int memoryMaxMessages = 20;

    /** 记忆 Redis TTL（秒），对齐设计文档 ai.memory-ttl=604800（7 天） */
    private int memoryTtlSeconds = 604800;

    /** 待确认操作 Redis TTL（秒） */
    private int pendingActionTtlSeconds = 300;

    /** text2sql 查询结果上限（强制 LIMIT） */
    private int queryMaxRows = 100;

    /** 只读 SQL 执行超时（秒） */
    private int queryTimeoutSeconds = 5;

    // ==================== Agent 稳定性与护轨 ====================

    /** 令牌桶限流（Redisson RRateLimiter） */
    private RateLimit rateLimit = new RateLimit();

    /** LLM 调用重试（指数退避） */
    private Retry retry = new Retry();

    /** 输入护轨 */
    private Guard guard = new Guard();

    /** 两级缓存（L1 Caffeine + L2 Redis） */
    private Cache cache = new Cache();

    /** 对话执行线程池 */
    private ChatExecutor chatExecutor = new ChatExecutor();

    /** 限流配置：按「用户 + 场景」维度的令牌桶 */
    @Data
    public static class RateLimit {
        /** 是否启用（Redis 不可用时自动降级放行） */
        private boolean enabled = true;
        /** 令牌桶容量（一个窗口内允许的请求数） */
        private long permits = 10;
        /** 窗口长度（秒） */
        private long intervalSeconds = 60;
    }

    /** LLM 调用重试配置 */
    @Data
    public static class Retry {
        /** 最大尝试次数（含首次调用） */
        private int maxAttempts = 3;
        /** 首次退避时长（毫秒） */
        private long initialBackoffMs = 500;
        /** 退避倍数 */
        private double multiplier = 2.0;
        /** 单次退避上限（毫秒） */
        private long maxBackoffMs = 4000;
    }

    /** 输入护轨配置 */
    @Data
    public static class Guard {
        /** 单条用户输入字符上限 */
        private int maxInputLength = 2000;
        /** 是否拦截疑似 Prompt 注入 / 越狱输入 */
        private boolean blockPromptInjection = true;
    }

    /** 两级缓存配置 */
    @Data
    public static class Cache {
        /** 总开关：关闭时所有缓存调用直接回源，不写不读 */
        private boolean enabled = true;
        /** L1 Caffeine 最大条目数 */
        private long l1MaxSize = 10000;
        /** L1 过期时间（秒）。必须短于 L2 —— L1 无法被其他实例失效，靠短 TTL 收敛脏读 */
        private long l1TtlSeconds = 5;
        /** L2 Redis 过期时间（秒） */
        private long l2TtlSeconds = 300;
    }

    /** 对话执行线程池配置（替代每个 SSE 请求裸 new Thread） */
    @Data
    public static class ChatExecutor {
        private int coreSize = 8;
        private int maxSize = 32;
        /** 有界队列容量，满则拒绝并回 SSE error，避免无界堆积导致 OOM */
        private int queueCapacity = 100;
        private int keepAliveSeconds = 60;
        /** 停机等待在途对话完成的时间（秒） */
        private int awaitTerminationSeconds = 30;
    }

    @Data
    public static class Model {
        /** 模型名 */
        private String modelName = "deepseek-chat";
        /** 覆盖全局 API Key（为空则用全局） */
        private String apiKey = "";
        /** 采样温度 */
        private Double temperature = 0.1;
        /** 最大输出 token */
        private Integer maxTokens = 2048;
    }
}
