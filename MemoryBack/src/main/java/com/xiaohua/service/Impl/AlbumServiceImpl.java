package com.xiaohua.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.xiaohua.dao.AlbumMapper;
import com.xiaohua.domain.Album;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.Operation;
import com.xiaohua.domain.VO.AlbumImageVO;
import com.xiaohua.domain.VO.PartAlbumVO;
import com.xiaohua.service.AlbumService;
import com.xiaohua.service.RecordService;
import com.xiaohua.utils.DateUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AlbumServiceImpl implements AlbumService {
    @Resource
    AlbumMapper albumMapper;
    @Resource
    RecordService recordService;
    @Resource
    DateUtil dateUtil;
    private String defaultAlbum = "/static/album/albumImg.png";

    /**
     * 添加相册
     * @param albumName
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAlbum(HttpServletRequest req, String albumName, Integer userId) {
        Album album = new Album();
        album.setAlbumImg(defaultAlbum);
        album.setUserId(userId);
        album.setAlbumName(albumName);
        System.out.println(album);
        albumMapper.addAlbum(album);
        recordService.addRecord(req, Operation.createAlbum.getName()+"\""+albumName+"\"", 1,userId);
    }

    @Override
    public void setAlbumCover( Integer albumId, Integer imageId) {
        albumMapper.uploadAlbum(albumId,imageId);
    }


    /**
     * 查询相册
     * @param userId
     * @return
     */
    @Override

    public List<PartAlbumVO> selectAllAlbum(Integer userId) {
        if(userId==null){
            return null;
        }
        List<PartAlbumVO> partAlbumVO = albumMapper.selectAllAlbum(userId);
        return partAlbumVO;
    }

    /**
     * 将图片添加至相册
     * @param albumId
     * @param imageId
     */
    @Override
    public void addImageToAlbum(HttpServletRequest req,Integer albumId, List<Integer> imageId,Integer userId) {
        albumMapper.addAlbumImage(albumId,imageId);
        String albumName = albumMapper.selectAlbum(albumId);

        recordService.addRecord(req, Operation.addImageToAlbum.getName()+"\""+albumName+"\"", imageId.size(),userId);
    }

    /**
     * 将图片移除相册
     * @param albumId
     * @param imageId
     */
    @Override
    public void removeImageToAlbum(HttpServletRequest req,Integer albumId, List<Integer> imageId,Integer userId) {
        System.out.println(albumId);
        System.out.println(imageId);

        albumMapper.removeAlbumImage(albumId,imageId);
        String albumName = albumMapper.selectAlbum(albumId);
        recordService.addRecord(req, "\""+albumName+"\""+Operation.deleteAlbumImage.getName(), imageId.size(),userId);
    }
    /**
     * 删除相册
     * @param albumIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(HttpServletRequest req,List<Integer> albumIds,Integer userId) {
        albumMapper.deleteAlbum(albumIds);
        albumMapper.deleteAlbumImageByAlbum(albumIds);
        recordService.addRecord(req,Operation.deleteAlbum.getName(), albumIds.size(),userId);
    }

    /**
     * 查询相册所有图片
     * @param albumId
     * @return
     * @throws ParseException
     */
    @Override
    public JSONObject selectAlbumImage(Integer albumId) throws ParseException {
        List<Image> images = albumMapper.selectAlbumImage(albumId);
        List<Date> dates = albumMapper.selectAlbumImageTime(albumId);
        System.out.println(images);
        List<AlbumImageVO> albumImageVOS = new ArrayList<>();
        List<String> previewList = new ArrayList<>();
        System.out.println(dates);
        for(int i=0;i<dates.size();i++){
            List<Image> img = new ArrayList<>();
           for(int j=0;j<images.size();j++){
//                懒惰表达式
               if(dates.get(i)==null||dates.get(i).equals(images.get(j).getImageDate())){
                   img.add(images.get(j));
                   previewList.add(images.get(j).getImageUrL());
                   images.remove(j);
                   j--;
               }
           }
           String time = "";
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日");
           if(dates.get(i)!=null) {
               String format = dateFormat.format(dates.get(i));
               String week = dateUtil.getWeek(dates.get(i));
               time = format+week;
           }
           else{
               time="其它时间";
           }
            albumImageVOS.add(new AlbumImageVO(time,img));
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("images",albumImageVOS);
        jsonObject.put("previewImageUrL",previewList);
        return jsonObject;
    }

}
