package com.xiaohua.domain;

/**
 * @program: MemoryBack
 * @description 返回图片位置可视化结果
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class VisualDataSite {
    private String imageSite;
    private Integer number;
    public VisualDataSite(){}
    public VisualDataSite(String imageSite, Integer number) {
        this.imageSite = imageSite;
        this.number = number;
    }

    public String getImageSite() {
        return imageSite;
    }

    public void setImageSite(String imageSite) {
        this.imageSite = imageSite;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "VisualDataSite{" +
                "imageSite='" + imageSite + '\'' +
                ", number=" + number +
                '}';
    }
}
