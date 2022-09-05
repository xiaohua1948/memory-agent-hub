package com.xiaohua.service.Impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.xiaohua.dao.*;
import com.xiaohua.domain.Operation;
import com.xiaohua.domain.VO.AIResultVO;
import com.xiaohua.domain.Album;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.VO.AllTimeTypeVO;
import com.xiaohua.domain.VO.ImageVO;
import com.xiaohua.service.ImageService;
import com.xiaohua.service.RecordService;
import com.xiaohua.utils.FileServeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ImageServiceImpl implements ImageService {

    @Resource
    private ImageMapper imageMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private AlbumMapper albumMapper;
    @Resource
    private RecycleMapper recycleMapper;
    @Resource
    private FileServeUtil fileServeUtil;
    @Resource
    private RecordService recordService;
//        当前系统时间
    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 普通上传
     * @param imageList
     * @param userId
     * @param albumId
     * @param albumName
     * @param imgType
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//事务控制
    public boolean uploadImage(HttpServletRequest req,List<Image> imageList, Integer userId, Integer albumId, String albumName, String imgType) throws Exception {
        //添加图片
        imageMapper.addImages(imageList);
        //返回图片id集合
        List<Integer> imageIds = new ArrayList<>();
        for (Image i : imageList) {
            imageIds.add(i.getImageId());
        }
        //添加中间用户图片表
        userMapper.addUserImage(userId,imageIds);
        //添加图片类型中间表
        imageMapper.addImageType(imageIds, imgType);
        //添加相册图片中间表
        System.out.println(albumName);
            if(!(albumName==null||albumName.equals(""))){

                if (albumId == 0) {
                    Album album = new Album();
                    album.setUserId(userId);
                    album.setAlbumName(albumName);
                    album.setAlbumImg(imageList.get(0).getCompressUrL());

                    albumMapper.addAlbum(album);
                    //添加记录
                    recordService.addRecord(req, Operation.createAlbum.getName()+"\""+albumName+"\"",1,userId);
                    albumMapper.addAlbumImage(album.getAlbumId(),imageIds);
                    recordService.addRecord(req, Operation.addImageToAlbum.getName()+"\""+albumName+"\"",imageIds.size(),userId);
                }
                else{
                    if(albumId > 0){
                        albumMapper.addAlbumImage(albumId,imageIds);
                        recordService.addRecord(req, Operation.addImageToAlbum.getName()+"\""+albumName+"\"",imageIds.size(),userId);
                    }
                }
            }



        return true;
    }

    /**
     * AI智能识别上传
     * @param imageList
     * @param userId
     * @param albumId
     * @param albumName
     * @param count
     * @return
     * @throws Exception
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public JSONArray AIUploadImage(HttpServletRequest req,List<Image> imageList, Integer userId, Integer albumId, String albumName, int count) throws Exception {
        //添加图片
        imageMapper.addImages(imageList);
        //返回图片id集合
        JSONArray data = new JSONArray();
        List<Integer> imageIds = new ArrayList<>();
        int i=0;
        for (Image image : imageList) {
            imageIds.add(image.getImageId());
            float fileSize = image.getImageSize();
            String filePath = image.getCompressUrL();
            List<AIResultVO> results = null;
            List<AIResultVO> res = null;
            results = fileServeUtil.imageGet(fileServeUtil.ServPathToAP(filePath));
            //是返回结果最少
            res = results.subList(0, Math.min(count, results.size() ));
            JSONObject js = new JSONObject();
            js.put("img","图片"+(i+1));
            for(int j=0;j<res.size();j++){
                String s = res.get(j).getValue()+res.get(j).getConfidence()+"%";
                js.put("res"+(j+1),s);
            }
            data.add(js);
            System.out.println(res);
            imageMapper.addImageTypes(image.getImageId(),res);
            i++;
        }
        //添加中间用户图片表
        userMapper.addUserImage(userId,imageIds);
        //添加相册图片中间表
            if (albumId == 0) {
                Album album = new Album();
                album.setAlbumName(albumName);
                album.setAlbumImg(imageList.get(0).getCompressUrL());
                album.setUserId(userId);
                albumMapper.addAlbum(album);
                //添加记录
                recordService.addRecord(req, Operation.createAlbum.getName()+"\""+albumName+"\"",1,userId);
                albumMapper.addAlbumImage(album.getAlbumId(),imageIds);
                recordService.addRecord(req, Operation.addImageToAlbum.getName()+"\""+albumName+"\"",imageIds.size(),userId);
            }
            else{
                if(albumId > 0){
                    albumMapper.addAlbumImage(albumId,imageIds);
                    recordService.addRecord(req, Operation.addImageToAlbum.getName()+"\""+albumName+"\"",imageIds.size(),userId);
                }
            }
        return data;
    }

    /**
     * 查询指定用户所有图片数量
     * @param userId
     * @return
     */
    @Override
    public Integer selectImageTotal(Integer userId) {
        Integer integer = imageMapper.selectImageTotalCount(userId);
        return integer;
    }

    /**
     * 分页查询指定用户所有图片
     * @param userId
     * @param currentPage
     * @param pageSize
     * @return
     */
    @Override
    public ImageVO selectAllImage(Integer userId, Integer currentPage, Integer pageSize) {
        PageHelper.startPage(currentPage,pageSize);
        List<Image> images = imageMapper.selectAllImage(userId);
        List<String> pres = new ArrayList<>();
        for (Image i:images){
            pres.add(i.getImageUrL());
        }
        Integer integer = imageMapper.selectImageTotalCount(userId);
        ImageVO imageVO = new ImageVO(images,pres,integer);
        return imageVO;
    }

    /**
     * 分页按类型查询指定用户所有图片
     * @param userId
     * @param currentPage
     * @param pageSize
     * @param imageType
     * @return
     */
    @Override
    public ImageVO selectAllImageByType(Integer userId, Integer currentPage, Integer pageSize,String imageType) {
        PageHelper.startPage(currentPage,pageSize);
        List<Image> images = imageMapper.selectAllImageByType(userId,imageType);
        List<String> pres = new ArrayList<>();
        for (Image i:images){
            pres.add(i.getImageUrL());
        }
        Integer integer = imageMapper.selectImageCountByType(userId,imageType);
        ImageVO imageVO = new ImageVO(images,pres,integer);
        return imageVO;
    }

    /**
     * 分页按时间查询指定用户所有图片
     * @param userId
     * @param currentPage
     * @param pageSize
     * @param imageDate
     * @return
     */
    @Override
    public ImageVO selectAllImageByTime(Integer userId, Integer currentPage, Integer pageSize, Date imageDate) {
        PageHelper.startPage(currentPage,pageSize);
        List<Image> images = imageMapper.selectAllImageByTime(userId,imageDate);
        List<String> pres = new ArrayList<>();
        for (Image i:images){
            pres.add(i.getImageUrL());
        }
        Integer integer = imageMapper.selectImageCountByTime(userId,imageDate);
        ImageVO imageVO = new ImageVO(images,pres,integer);
        return imageVO;
    }

    /**
     * 查询图片时间和类型
     * @param userId
     * @return
     */
    @Override
    public AllTimeTypeVO selectTimeType(Integer userId) {
        List<Date> dates = imageMapper.selectAllImageTime(userId);
        //将null时间删掉
        for(int i=0;i<dates.size();i++){
            if(dates.get(i)==null){
                dates.remove(i);
            }
        }
        List<String> strings = imageMapper.selectAllImageType(userId);
        AllTimeTypeVO allTimeTypeVO = new AllTimeTypeVO(strings,dates);
        return allTimeTypeVO;
    }

    /**
     * 删除图片，加入回收站
     * @param userId
     * @param imageId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteImage(Integer userId,List<Integer> imageId) {
        imageMapper.deleteUserImage(userId,imageId);
        recycleMapper.addImageToRecycle(userId,imageId);
        albumMapper.deleteAlbumImageByImgId(imageId);
    }


}
