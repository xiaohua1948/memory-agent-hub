package com.memory.xiaohua.ai.agent;

import lombok.Data;

import java.util.List;

/**
 * Router 拆解出的执行计划
 *
 * @author xiaohua
 */
@Data
public class AgentPlan {

    /** 有序子任务列表 */
    private List<Step> steps;

    @Data
    public static class Step {
        /** 步骤序号（从 1 开始） */
        private int step;
        /** 目标 Agent：QUERY / ALBUM / CODE */
        private String agent;
        /** 该步骤的输入（自然语言描述） */
        private String input;
        /** 依赖的前置步骤序号（null 表示无依赖） */
        private Integer dependsOn;
        /** 是否含写操作（需要 human-in-the-loop） */
        private boolean write;
    }
}
