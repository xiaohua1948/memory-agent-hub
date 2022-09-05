package com.xiaohua;

import com.xiaohua.dao.AlbumMapper;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.VO.AlbumImageVO;
import com.xiaohua.domain.VO.PartAlbumVO;
import com.xiaohua.service.AlbumService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class Album {

    @Resource
    AlbumService albumService;
    @Resource
    AlbumMapper albumMapper;
    @Test
    void selectAlbumPart(){

//        List<AlbumImageVO> albumImageVOS = albumMapper.selectAlbumImage(1);
//        System.out.println(albumImageVOS);

    }
}
