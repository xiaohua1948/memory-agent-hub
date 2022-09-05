package com.xiaohua.domain.VO;

import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import java.util.List;
/**
 * @program: MemoryBack
 * @description 返回所有图片的时间和类型结果
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class AllTimeTypeVO {
    private List<String> imageType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JSONField(format = "yyyy-MM-dd")
    private List<Date> imageDate;
    public AllTimeTypeVO(){
    }
    public AllTimeTypeVO(List<String> imageType, List<Date> imageDate) {
        this.imageType = imageType;
        this.imageDate = imageDate;
    }

    public List<String> getImageType() {
        return imageType;
    }

    public void setImageType(List<String> imageType) {
        this.imageType = imageType;
    }

    public List<Date> getImageDate() {
        return imageDate;
    }

    public void setImageDate(List<Date> imageDate) {
        this.imageDate = imageDate;
    }

    @Override
    public String toString() {
        return "AllTimeTypeVO{" +
                "imageType=" + imageType +
                ", imageDate=" + imageDate +
                '}';
    }
}
