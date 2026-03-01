package com.memory.xiaohua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.Location;
import com.memory.xiaohua.model.vo.visual.VisualLocationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {


    List<VisualLocationVO> selectAllLocation(@Param("userId") Long userId);
}
