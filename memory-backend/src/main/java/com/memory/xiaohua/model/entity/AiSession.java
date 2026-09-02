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
 * AI 智能助手会话实体
 *
 * <p>表结构对齐《AI智能助手-优化计划.md》§5.1：自增主键 id + 业务唯一 session_id +
 * 逻辑删除 deleted。sessionId 用于业务与记忆隔离（memoryId）。
 *
 * @author xiaohua
 */
@Data
@TableName("ai_session")
public class AiSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话业务ID（UUID，唯一） */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** Agent 类型：data / code / mixed */
    private String agentType;

    /** 会话标题（AI 生成，兜底取首条消息前 12 字） */
    private String title;

    /** 会话状态：0 进行中 1 已结束 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0 否 1 是 */
    @TableLogic
    private Integer deleted;
}
