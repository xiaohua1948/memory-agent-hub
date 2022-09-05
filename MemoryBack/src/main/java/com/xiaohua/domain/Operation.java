package com.xiaohua.domain;

/**
 * @program: MemoryBack
 * @description 操作记录枚举
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public enum Operation {
    uploadImage("上传图片",1),
    AIUploadImage("AI识别上传",2),
    downloadImage("下载图片",3),
    deleteImage("彻底删除图片",4),
    addRecycle("加入回收站",5),
    recoverImage("恢复图片",6),
    createAlbum("创建相册",7),
    deleteAlbum("删除相册",8),
    addImageToAlbum("添加图片至相册",9),
    deleteAlbumImage("相册移除图片",10);
    private String name;
    private Integer number;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    Operation(String name, Integer number) {
        this.name = name;
        this.number = number;
    }
}
