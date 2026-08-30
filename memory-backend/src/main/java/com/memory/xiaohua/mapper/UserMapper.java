package com.memory.xiaohua.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.User;
import com.memory.xiaohua.model.vo.entity.UserInfoVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    List<UserInfoVO> selectAllUserInfo(@Param("role") String role);

    List<UserInfoVO> selectAllAdmin(@Param("role") String role);

    @Select("SELECT COUNT(*) FROM user WHERE role = 'user'")
    Long countNormalUsers();
}
