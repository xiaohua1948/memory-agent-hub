package com.memory.xiaohua.model.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * AI 聊天请求
 *
 * <p>对齐《AI智能助手-优化计划.md》§3.2 接口契约：
 * {@code POST /ai/chat { sessionId?, agentType, content, fileIds[] }}。
 * 历史消息不再由前端全量回传，由后端基于 sessionId 从记忆/DB 自动装配。
 *
 * <p>输入护轨第一道：声明式参数校验（长度上限与下方 {@code ai.guard.max-input-length}
 * 保持一致，双重防线，避免绕过 Controller 直接调用 Service 时失去长度保护）。
 *
 * @author xiaohua
 */
@Data
public class AiChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID（为空则新建会话） */
    private String sessionId;

    /** 用户输入内容（纯文本） */
    @NotBlank(message = "消息不能为空")
    @Size(max = 2000, message = "消息长度不能超过2000字")
    private String content;

    /** 会话选中的照片上下文（fileId 列表，Code Agent 使用） */
    private List<String> fileIds;

    /** Agent 类型提示（可选，为空走 Router 分诊） */
    private String agentType;
}
