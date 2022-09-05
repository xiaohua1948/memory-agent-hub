package com.xiaohua;

import com.xiaohua.dao.ImageMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class Image {
    @Resource
    private ImageMapper imageMapper;
    @Test
    void addImages(){
        List<com.xiaohua.domain.Image> list = new ArrayList<>();
        com.xiaohua.domain.Image image = new com.xiaohua.domain.Image();
        image.setImageName("1");
        list.add(image);
        imageMapper.addImages(list);
        System.out.println(list);
    }
    @Test
    void selectImage() throws ParseException {
//        List<String> strings = imageMapper.selectAllImageType(1);
//        List<Date> dates = imageMapper.selectAllImageTime(1);
//        List<com.xiaohua.domain.Image> s = imageMapper.selectAllImageByType(1, "人物");

//                "2022-08-17"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date date = sdf.parse("2022-08-16");
        List<com.xiaohua.domain.Image> images = imageMapper.selectAllImageByTime(1, date);
        System.out.println(images);
    }
}
