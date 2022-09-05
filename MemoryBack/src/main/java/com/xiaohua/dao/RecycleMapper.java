package com.xiaohua.dao;

import com.xiaohua.domain.Image;
import com.xiaohua.domain.Recycle;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RecycleMapper {
    /**
     * 将指定用户的图片加入回收站
     * @param userId
     * @param imageIds
     */
    void addImageToRecycle(Integer userId,@Param("list") List<Integer> imageIds );

    /**
     * 查询回收站内所有图片
     * @param userId
     * @return
     */
    @Select(" select image.id,compressUrL,(30 - datediff(now(),recycleDate)) as day from image inner join recycle on image.id=imageId where userId=#{userId} order by day desc")
    @Results(id="ImageResultMap" ,value = {
            @Result(property = "imageId",column = "id"),
    })
    List<Recycle> selectAll(Integer userId);

    /**
     * 删除回收站
     * @param userId
     * @param imageId
     */
    void deleteRecycle(Integer userId,@Param("list")List<Integer> imageId);




}
