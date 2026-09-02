package com.memory.xiaohua.model.dto.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 执行确认请求（human-in-the-loop 用户确认）
 *
 * @author xiaohua
 */
@Data
public class AiExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待确认操作ID */
    private String actionId;
}
