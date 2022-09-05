package com.xiaohua.domain;

import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
/**
 * @program: MemoryBack
 * @description 图片实体类
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class Image {
    private Integer imageId;
    private String imageName;
    private Long imageSize;
    private String imageSite;
    private String imageUrL;
    private String compressUrL;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JSONField(format = "yyyy-MM-dd")
    private Date imageDate;

    @Override
    public String toString() {
        return "Image{" +
                "imageId=" + imageId +
                ", imageName='" + imageName + '\'' +
                ", imageSize=" + imageSize +
                ", imageSite='" + imageSite + '\'' +
                ", imageUrL='" + imageUrL + '\'' +
                ", compressUrL='" + compressUrL + '\'' +
                ", imageDate=" + imageDate +
                '}';
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Long getImageSize() {
        return imageSize;
    }

    public void setImageSize(Long imageSize) {
        this.imageSize = imageSize;
    }

    public String getImageSite() {
        return imageSite;
    }

    public void setImageSite(String imageSite) {
        this.imageSite = imageSite;
    }

    public String getImageUrL() {
        return imageUrL;
    }

    public void setImageUrL(String imageUrL) {
        this.imageUrL = imageUrL;
    }

    public String getCompressUrL() {
        return compressUrL;
    }

    public void setCompressUrL(String compressUrL) {
        this.compressUrL = compressUrL;
    }

    public Date getImageDate() {
        return imageDate;
    }

    public void setImageDate(Date imageDate) {
        this.imageDate = imageDate;
    }

    public Image(Integer imageId, String imageName, Long imageSize, String imageSite, String imageUrL, String compressUrL, Date imageDate) {
        this.imageId = imageId;
        this.imageName = imageName;
        this.imageSize = imageSize;
        this.imageSite = imageSite;
        this.imageUrL = imageUrL;
        this.compressUrL = compressUrL;
        this.imageDate = imageDate;
    }

    public Image() {

    }


}
