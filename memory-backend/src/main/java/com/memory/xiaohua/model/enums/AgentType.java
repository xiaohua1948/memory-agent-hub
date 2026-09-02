package com.memory.xiaohua.model.enums;

/**
 * AI Agent 类型枚举
 *
 * @author xiaohua
 */
public enum AgentType {

    /** 照片查询（text2sql，纯读） */
    QUERY("QUERY", "照片查询"),

    /** 相册管理（含写操作，human-in-the-loop） */
    ALBUM("ALBUM", "相册管理"),

    /** 网页生成（纯读，Code Agent） */
    CODE("CODE", "网页生成"),

    /** 混合（由 Router 分诊） */
    MIXED("MIXED", "混合");

    private final String code;
    private final String desc;

    AgentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AgentType fromCode(String code) {
        for (AgentType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return MIXED;
    }
}
