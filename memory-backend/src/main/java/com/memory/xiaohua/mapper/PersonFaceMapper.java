package com.memory.xiaohua.mapper;

import com.memory.xiaohua.model.entity.PersonFace;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 人物脸部关联表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-10
 */
public interface PersonFaceMapper extends BaseMapper<PersonFace> {
        void insertPersonFaces(@Param("userId") Long userId,@Param("personId") Long oldPersonId,@Param("faceIds") List<Long> faceIds);
}
