package com.xiaohua.domain.VO;


/**
 * @program: MemoryBack
 * @description 返回阿里云图像识别的结果
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class AIResultVO {
    private Float confidence;
    private String value;

    @Override
    public String toString() {
        return "AIResultVO{" +
                "confidence=" + confidence +
                ", value='" + value + '\'' +
                '}';
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public AIResultVO(){}
    public AIResultVO(Float confidence, String value) {
        this.confidence = confidence;
        this.value = value;
    }
}
