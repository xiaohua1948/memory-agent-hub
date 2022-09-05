package com.xiaohua.dao;

import com.xiaohua.domain.Album;
import com.xiaohua.domain.Image;
import com.xiaohua.domain.VO.AlbumImageVO;
import com.xiaohua.domain.VO.PartAlbumVO;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface AlbumMapper {

    /**
     * 创建相册
     * @param album
     */
    @Insert("insert into album values (null,#{albumName},#{albumTheme},#{albumContext},#{albumImg},#{backgroundImage},#{albumMusic},#{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "albumId")
    void addAlbum(Album album);

    @Update("update album set albumImg = (select compressUrL from image where image.id=#{imageId}) where album.id =#{albumId} ")
    void uploadAlbum(Integer albumId,Integer imageId);

    @Select("select albumName from album where id=#{albumId}")
    String selectAlbum(Integer albumId);

    /**
     * 根据id删除相册
     * @param albumIds
     */
    void deleteAlbum(@Param("list") List<Integer> albumIds);


    /**
     * 根据相册id删除中间表
     * @param albumIds
     */
    void deleteAlbumImageByAlbum(@Param("list") List<Integer> albumIds);
    /**
     * 根据图片id删除相册图片中间表
     * @param imageIds
     */
    void deleteAlbumImageByImgId(@Param("list") List<Integer> imageIds);

    /**
     * 添加相册图片中间表
     * @param imageIds
     */
    void addAlbumImage(int albumId,@Param("list") List<Integer> imageIds);

    /**
     * 移除相册图片中间表
     * @param imageIds
     */
    void removeAlbumImage(int albumId,@Param("list") List<Integer> imageIds);



    /**
     * 查询用户相册部分信息
     * @param userId
     * @return
     */
    @Select(" select album.id,albumName,albumImg,count(DISTINCT album_image.imageId) as imageNumber from album  left join album_image on album_image.albumId = album.id  where album.userId=#{userId}  group by album.id")
    @Results(id="AlbumResultMap" ,value = {
            @Result(property = "albumId",column = "id"),
    })
    List<PartAlbumVO> selectAllAlbum(Integer userId);


    /**
     * 根据相册id查询所有图片
     * @param albumId
     * @return
     */
   @Select("select id as imageId ,compressUrl,imageUrL,imageDate,imageName from image  where id in (select imageId from album_image where albumId=#{albumId}) ")
    List<Image> selectAlbumImage(Integer albumId);

    /**
     * 根据相册id查询所有时间段
     * @param albumId
     * @return
     */
    @Select("select imageDate from image where id in (select imageId from album_image where albumId=#{albumId}) group by imageDate order by imageDate desc ")
    List<Date> selectAlbumImageTime(Integer albumId);


}
