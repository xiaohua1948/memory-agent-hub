package com.memory.xiaohua.model.enums;

public enum PersonRelation {
    SELF("我自己"),
    DEAR("亲爱的"),
    CHILD("孩子"),
    FATHER("爸爸"),
    MOTHER("妈妈"),
    OTHER_RELATIVE("其他亲属"),
    FRIEND("朋友"),
    CLASSMATE("同学"),
    COLLEAGUE("同事"),
    NO_RELATION("无关系");

    private final String displayName;

    PersonRelation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // 可选：通过中文名称获取枚举值的方法
    public static PersonRelation fromDisplayName(String displayName) {
        for (PersonRelation relation : values()) {
            if (relation.displayName.equals(displayName)) {
                return relation;
            }
        }
        return null; // 默认返回无关系
    }
}