package com.xiaohua.domain.VO;

import com.xiaohua.domain.Image;

import java.util.List;

/**
 * @program: MemoryBack
 * @description 返回图片的压缩UrL和原品质UrL
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class ImageVO {
    private List<Image> images;
    private List<String> previewImageUrL;
    private Integer totalCount;

    @Override
    public String toString() {
        return "ImageVO{" +
                "images=" + images +
                ", previewImageUrL=" + previewImageUrL +
                ", totalCount=" + totalCount +
                '}';
    }

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public List<String> getPreviewImageUrL() {
        return previewImageUrL;
    }

    public void setPreviewImageUrL(List<String> previewImageUrL) {
        this.previewImageUrL = previewImageUrL;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    public ImageVO(){}
    public ImageVO(List<Image> images, List<String> previewImageUrL, Integer totalCount) {
        this.images = images;
        this.previewImageUrL = previewImageUrL;
        this.totalCount = totalCount;
    }
}
