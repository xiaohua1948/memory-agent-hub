package com.memory.xiaohua.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 创建会话请求
 *
 * @author xiaohua
 */
@Data
public class AiCreateSessionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Agent 类型：DATA / CODE / MIXED */
    private String agentType;
}
