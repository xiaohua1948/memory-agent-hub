package com.xiaohua.domain;

/**
 * @program: MemoryBack
 * @description 返回图片类型可视化结果
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class VisualDataType {
    private String name;
    private Integer value;
    public VisualDataType(){}
    public VisualDataType(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "VisualDataType{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }
}
