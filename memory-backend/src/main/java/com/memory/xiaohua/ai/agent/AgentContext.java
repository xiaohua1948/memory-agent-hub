package com.memory.xiaohua.ai.agent;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 编排上下文（TaskContext）
 *
 * <p>在 Router 拆解出的多步任务间透传中间结果。子 Agent 执行完把结果写入，
 * 下游子 Agent 通过符号引用（如 $step1.photoIds）读取，实现跨 Agent 边界的数据传递。
 *
 * @author xiaohua
 */
@Data
public class AgentContext {

    /** 当前用户 */
    private Long userId;

    /** 当前会话 */
    private String sessionId;

    /** 用户原始输入 */
    private String userInput;

    /** 历史对话文本（由记忆装配，注入 Router / 任务 prompt 以支持多轮指代） */
    private String history;

    /** 用户选中的照片上下文（Code Agent 使用） */
    private java.util.List<String> contextFileIds;

    /** 步骤结果存储（stepIndex -> key -> value） */
    private final Map<Integer, Map<String, Object>> stepResults = new LinkedHashMap<>();

    public void putResult(int stepIndex, String key, Object value) {
        stepResults.computeIfAbsent(stepIndex, k -> new LinkedHashMap<>()).put(key, value);
    }

    public Object getResult(int stepIndex, String key) {
        Map<String, Object> m = stepResults.get(stepIndex);
        return m == null ? null : m.get(key);
    }
}
