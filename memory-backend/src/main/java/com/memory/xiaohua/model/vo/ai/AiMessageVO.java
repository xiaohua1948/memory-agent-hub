package com.memory.xiaohua.model.vo.ai;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 消息 VO
 *
 * @author xiaohua
 */
@Data
public class AiMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String sessionId;

    /** user / assistant / tool */
    private String role;

    /** text / photos / html / pending_action */
    private String contentType;

    /** 纯文本内容 */
    private String content;

    /** 附加数据 JSON（结构化内容） */
    private String extra;

    private LocalDateTime createTime;
}
