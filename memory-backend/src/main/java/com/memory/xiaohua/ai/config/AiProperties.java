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
