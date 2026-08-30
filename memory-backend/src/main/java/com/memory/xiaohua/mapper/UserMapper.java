package com.memory.xiaohua.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-18
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
