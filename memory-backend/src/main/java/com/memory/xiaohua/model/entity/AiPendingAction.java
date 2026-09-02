package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 待确认写操作实体（human-in-the-loop 二次确认）
 *
 * <p>表结构对齐《AI智能助手-优化计划.md》§5.1：自增主键 id + 业务唯一 action_id。
 * Redis 为主存储（TTL 自动过期），DB 兜底审计。
 *
 * @author xiaohua
 */
@Data
@TableName("ai_pending_action")
public class AiPendingAction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 待确认操作ID（UUID，唯一） */
    private String actionId;

    /** 会话业务ID */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 操作类型：CREATE_ALBUM / ADD_PICTURES / REMOVE_PICTURES / UPDATE_ALBUM / DELETE_ALBUM */
    private String actionType;

    /** 执行参数 JSON */
    private String execParams;

    /** 状态：PENDING / CONFIRMED / CANCELLED / EXPIRED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 确认时间 */
    private LocalDateTime confirmTime;
}
