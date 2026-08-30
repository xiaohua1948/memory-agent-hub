package com.memory.xiaohua.mapper;

import com.memory.xiaohua.model.entity.Face;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 人像特征表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
public interface FaceMapper extends BaseMapper<Face> {
    List<Long> selectFaceIdByFileIds(@Param("userId") Long userId, @Param("fileIds") List<String> fileIds);

}
