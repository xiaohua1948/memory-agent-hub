package com.xiaohua.domain;

/**
 * @program: MemoryBack
 * @description 回收站
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class Recycle {
    private Integer imageId;
    private String compressUrL;
    private Integer day;

    public Recycle(){}
    public Recycle(Integer imageId, String compressUrL, Integer day) {
        this.imageId = imageId;
        this.compressUrL = compressUrL;
        this.day = day;
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    public String getCompressUrL() {
        return compressUrL;
    }

    public void setCompressUrL(String compressUrL) {
        this.compressUrL = compressUrL;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    @Override
    public String toString() {
        return "Recycle{" +
                "imageId=" + imageId +
                ", compressUrL='" + compressUrL + '\'' +
                ", day=" + day +
                '}';
    }
}
