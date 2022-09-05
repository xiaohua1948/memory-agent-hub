package com.xiaohua.domain;

/**
 * @program: MemoryBack
 * @description 相册实体类
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class Album {
    private Integer albumId;
    private String albumName;
    private String albumTheme;
    private String albumContext;
    private String albumImg;
    private String backgroundImage;
    private String albumMusic;
    private Integer userId;



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

    public String getAlbumTheme() {
        return albumTheme;
    }

    public void setAlbumTheme(String albumTheme) {
        this.albumTheme = albumTheme;
    }

    public String getAlbumContext() {
        return albumContext;
    }

    public void setAlbumContext(String albumContext) {
        this.albumContext = albumContext;
    }

    public String getAlbumImg() {
        return albumImg;
    }

    public void setAlbumImg(String albumImg) {
        this.albumImg = albumImg;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(String backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public String getAlbumMusic() {
        return albumMusic;
    }

    public void setAlbumMusic(String albumMusic) {
        this.albumMusic = albumMusic;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Album(){
    }
    public Album(Integer albumId, String albumName, String albumTheme, String albumContext, String albumImg, String backgroundImage, String albumMusic, Integer userId) {
        this.albumId = albumId;
        this.albumName = albumName;
        this.albumTheme = albumTheme;
        this.albumContext = albumContext;
        this.albumImg = albumImg;
        this.backgroundImage = backgroundImage;
        this.albumMusic = albumMusic;
        this.userId = userId;
    }
    @Override
    public String toString() {
        return "Album{" +
                "albumId=" + albumId +
                ", albumName='" + albumName + '\'' +
                ", albumTheme='" + albumTheme + '\'' +
                ", albumContext='" + albumContext + '\'' +
                ", albumImg='" + albumImg + '\'' +
                ", backgroundImage='" + backgroundImage + '\'' +
                ", albumMusic='" + albumMusic + '\'' +
                ", userId=" + userId +
                '}';
    }
}
