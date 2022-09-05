package com.xiaohua.service;

import com.alibaba.fastjson.JSONArray;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.VO.AllTimeTypeVO;
import com.xiaohua.domain.VO.ImageVO;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

public interface ImageService {

    /**
     * 普通上传
     *
     * @param
     * @param imageList
     * @param userId
     * @param albumId
     * @param albumName
     * @param imgType
     * @return
     */
    boolean uploadImage(HttpServletRequest req,List<Image> imageList, Integer userId, Integer albumId, String albumName, String imgType) throws Exception;

    Integer selectImageTotal(Integer userId);

    JSONArray AIUploadImage(HttpServletRequest req,List<Image> imageList, Integer userId, Integer albumId, String albumName, int count) throws Exception;

    ImageVO selectAllImage(Integer userId, Integer currentPage, Integer pageSize);

    ImageVO selectAllImageByType(Integer userId,Integer currentPage,Integer pageSize,String imageType);

    ImageVO selectAllImageByTime(Integer userId, Integer currentPage, Integer pageSize, Date imageDate);

    AllTimeTypeVO selectTimeType(Integer userId);

    void deleteImage(Integer userId,List<Integer> imageId);
}
