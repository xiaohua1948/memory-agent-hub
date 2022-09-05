package com.xiaohua.service;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.domain.VO.PartAlbumVO;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

@Service
public interface AlbumService {

    void addAlbum(HttpServletRequest req,String albumName, Integer userId);

    void setAlbumCover(Integer albumId,Integer imageId);

    List<PartAlbumVO> selectAllAlbum(Integer userId);

    void addImageToAlbum(HttpServletRequest req,Integer albumId,List<Integer> imageId,Integer userId);

    void removeImageToAlbum(HttpServletRequest req,Integer albumId, List<Integer> imageId,Integer userId);

    void deleteAlbum(HttpServletRequest req,List<Integer> albumId,Integer userId);

    JSONObject selectAlbumImage(Integer albumId) throws ParseException;
}
