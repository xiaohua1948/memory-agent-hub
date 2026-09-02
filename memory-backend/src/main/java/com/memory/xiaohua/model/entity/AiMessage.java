package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 智能助手消息实体
 *
 * <p>表结构对齐《AI智能助手-优化计划.md》§5.1：
 * <ul>
 *   <li>{@code content} 存纯文本（供记忆回灌，保证记忆里只有干净的 user/assistant 文本）；</li>
 *   <li>{@code contentType} 区分 text / photos / html / pending_action；</li>
 *   <li>{@code extra} JSON 存结构化内容（照片结果 / HTML / actionId 等），不进记忆。</li>
 * </ul>
 *
 * @author xiaohua
 */
@Data
@TableName("ai_message")
public class AiMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话业务ID */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 角色：user / assistant / tool */
    private String role;

    /** 纯文本内容（记忆回灌数据源） */
    private String content;

    /** 内容类型：text / photos / html / pending_action */
    private String contentType;

    /** 附加数据 JSON（照片 ids / HTML / actionId 等结构化内容，不进记忆） */
    private String extra;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
