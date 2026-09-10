package com.memory.xiaohua.model.vo.ai;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 待确认操作 VO（推送给前端渲染确认卡片）
 *
 * @author xiaohua
 */
@Data
public class PendingActionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作ID */
    private String actionId;

    /**
     * 所属执行步骤序号（关联前端步骤条）。
     *
     * <p>用户确认后，前端据此把对应步骤从「待确认」置为「已完成」——
     * 一轮里若有多个写操作，也能精确标记而不是全部置灰。
     */
    private Integer stepId;

    /** 操作类型：CREATE_ALBUM / ADD_PICTURES ... */
    private String actionType;

    /** 操作标题（卡片标题） */
    private String title;

    /** 影响范围描述 */
    private String description;

    /** 预览数据（JSON，如相册名、照片数、缩略图等） */
    private Object preview;
}
