package com.memory.xiaohua.model.dto.ai;

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
 * @author xiaohua
 */
@Data
public class AiChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID（为空则新建会话） */
    private String sessionId;

    /** 用户输入内容（纯文本） */
    private String content;

    /** 会话选中的照片上下文（fileId 列表，Code Agent 使用） */
    private List<String> fileIds;

    /** Agent 类型提示（可选，为空走 Router 分诊） */
    private String agentType;
}
