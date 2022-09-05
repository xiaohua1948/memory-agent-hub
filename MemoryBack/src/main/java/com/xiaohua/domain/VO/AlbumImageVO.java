package com.xiaohua.domain.VO;
import com.xiaohua.domain.Image;
import java.util.List;

/**
 * @program: MemoryBack
 * @description 返回相册内部图片实体和时间段
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class AlbumImageVO {
    private String time;
    List<Image> image;

    public AlbumImageVO(){}
    public AlbumImageVO(String time, List<Image> image) {
        this.time = time;
        this.image = image;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<Image> getImage() {
        return image;
    }

    public void setImage(List<Image> image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "AlbumImageVO{" +
                "time='" + time + '\'' +
                ", images=" + image +
                '}';
    }
}
