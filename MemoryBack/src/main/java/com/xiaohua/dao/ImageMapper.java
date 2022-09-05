package com.xiaohua.dao;

import com.xiaohua.domain.VO.AIResultVO;
import com.xiaohua.domain.Image;
import org.apache.ibatis.annotations.*;


import java.util.Date;
import java.util.List;
@Mapper
public interface ImageMapper {

    /**
     * 添加单个图片
     * @param image
     */
    @Insert("insert into image values(null,#{imageName},#{imageSize},#{imageSite},#{imageUrL},#{imageDate}) ")
    @Options(useGeneratedKeys = true, keyProperty = "imageId")
    void addImage(Image image);
    /**
     *
     * @param imageList 图片对象集合
     * @return 自动将插入成功的图片id封装到传入参数
     */
    void addImages(@Param("list")List<Image> imageList);

    /**
     * 图片类型中间表
     * @param imageIds
     * @param imageType
     */
    void addImageType(@Param("list") List<Integer> imageIds, String imageType);

    /**
     * 单个图片类型多个类型
     * @param imageId
     * @param imageType
     */
    void addImageTypes(Integer imageId,@Param("list")List<AIResultVO> imageType);

    /**
     * 1.查询指定用户所有的图片类型
     * @param userId
     * @return
     */
    @Select("select imageType from image_type where imageId in (select imageId from user_image where userId=#{userId}) group by imageType")
    List<String> selectAllImageType(Integer userId);

    /**
     * 2.查询指定用户所有的图片时间
     * @param userId
     * @return
     */
    @Select("select imageDate from image where id in (select imageId from user_image where userId=#{userId}) group by imageDate")
    List<Date> selectAllImageTime(Integer userId);



    /**
     * 1.分页查询所有图片
     * @param userId
     * @return
     */
    @Select("select * from image where id in (select imageId from user_image where userId=#{userId})")
    @Results(id="ImageResultMap" ,value = {
            @Result(property = "imageId",column = "id"),
    })
    List<Image> selectAllImage(Integer userId);

    /**
     * 2.分页查询所有符合类型的图片
     * @param userId
     * @param imageType
     * @return
     */
    @Select("select * from image where (id in (select imageId from user_image where userId=#{userId}) and id in (select imageId from image_type where imageType=#{imageType}))" )
    @ResultMap(value = "ImageResultMap")
    List<Image> selectAllImageByType(Integer userId,String imageType);

    /**
     * 3.分页查询所有符合时间的图片
     * @param userId
     * @param imageDate
     * @return
     */
    @Select("select * from image where imageDate = #{imageDate} and id in (select imageId from user_image where userId=#{userId})" )
    @ResultMap(value = "ImageResultMap")
    List<Image> selectAllImageByTime(Integer userId, Date imageDate);

    /**
     * 查询指定用户图片所有数量
     * @param userId
     * @return
     */
    @Select("select count(*) from image where id in (select imageId from user_image where userId=#{userId})")
    Integer selectImageTotalCount(Integer userId);

    /**
     * 查询指定瀛湖指定类型的图片数量
     * @param userId
     * @param imageType
     * @return
     */
    @Select("select count(*) from image where (id in (select imageId from user_image where userId=#{userId}) and id in (select imageId from image_type where imageType=#{imageType}))")
    Integer selectImageCountByType(Integer userId,String imageType);

    /**
     * 查询指定瀛湖指定时间的图片数量
     * @param userId
     * @param imageDate
     * @return
     */
    @Select("select count(*) from image where imageDate = #{imageDate} and id in (select imageId from user_image where userId=#{userId})")
    Integer selectImageCountByTime(Integer userId,Date imageDate);


    /**
     * 删除用户图片中间表，目的将图片加入回收站
     * @param userId
     * @param imageId
     */
    void deleteUserImage(Integer userId,@Param("list")List<Integer> imageId);

    /**
     * 1.根据图片id查询图片url
     * @param imageId
     */
    List<Image> selectImageByIds(@Param("list")List<Integer> imageId);
    /**
     * 2.彻底删除图片
     */
    void deleteImage(@Param("list")List<Integer> imageId);

    /**
     * 3.删除图片对应的类型
     * @param imageId
     */
    void deleteImageType(@Param("list")List<Integer> imageId);



//    void updateImage(@Param("list")List<Image> images);



}
