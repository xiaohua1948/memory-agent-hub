package com.xiaohua.domain.VO;

/**
 * @program: MemoryBack
 * @description 返回所有相册基本信息(封面,名字,图片数量)
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class PartAlbumVO {
    private Integer albumId;
    private String albumName;
    private String albumImg;
    private Integer imageNumber;

    public PartAlbumVO(){

    }
    public PartAlbumVO(Integer albumId, String albumName, String albumImg, Integer imageNumber) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.albumImg = albumImg;
        this.imageNumber = imageNumber;
    }

    public Integer getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Integer albumId) {
        this.albumId = albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getAlbumImg() {
        return albumImg;
    }

    public void setAlbumImg(String albumImg) {
        this.albumImg = albumImg;
    }

    public Integer getImageNumber() {
        return imageNumber;
    }

    public void setImageNumber(Integer imageNumber) {
        this.imageNumber = imageNumber;
    }

    @Override
    public String toString() {
        return "PartAlbumVO{" +
                "albumId=" + albumId +
                ", albumName='" + albumName + '\'' +
                ", albumImg='" + albumImg + '\'' +
                ", imageNumber=" + imageNumber +
                '}';
    }
}
