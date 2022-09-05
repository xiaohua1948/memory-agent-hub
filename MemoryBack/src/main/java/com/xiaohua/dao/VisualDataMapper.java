package com.xiaohua.dao;

import com.xiaohua.domain.VisualDataSite;
import com.xiaohua.domain.VisualDataType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VisualDataMapper {

    /**
     * 查询图片类型分布可视化数据
     * @param userId
     * @return
     */
    List<VisualDataType> selectVisualDataType(Integer userId);

    /**
     * 查询图片位置可视化数据
     * @param userId
     * @return
     */
    List<VisualDataSite> selectVisualDataSite(Integer userId);

    /**
     * 查询用户总容量
     * @param userId
     * @return
     */
    @Select("select capacity from user where id=#{userId}")
    Integer selectCapacity(Integer userId);

    /**
     * 查询用户图片所占用内存
     * @param userId
     * @return
     */
    @Select("select sum(imageSize) from image where  id in  (select imageId from user_image where userId=#{userId})")
    Integer selectImageSumSize(Integer userId);
}
