package com.memory.xiaohua.model.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话 VO
 *
 * @author xiaohua
 */
@Data
public class AiSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String agentType;

    private String title;

    private LocalDateTime updateTime;
}
