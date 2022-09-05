package com.xiaohua.dao;

import com.xiaohua.domain.User;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface UserMapper {

    List<User> selectAllUser();
    /**
     * 用户注册
     * @param user
     * @return
     */
    @Insert("insert into user values (null,#{userName},#{passWord},#{sex},#{email},#{phone},#{city},#{birthday},#{capacity},#{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "userId")
    int add(User user);

    /**
     * 用户账号查询，防止重复注册
     * @param UserName
     * @return
     */
    @Select("select userName from user where userName = #{userName}")
    String selectUserName(String UserName);
    /**
     *用户id查询,用于账号登录
     * @param user
     * @return
     */
    @Select("select id from user where (email=#{userName} or userName = #{userName})and passWord = #{passWord}")
    Integer selectUserId(User user);
    /**
     * 用户查询
     * @param userId
     * @return
     */
    @Select("select * from user where id=#{userId}")
    @ResultType(User.class)
    @Results(id="UserResultMap" ,value = {
            @Result(property = "userId",column = "id"),
    })
    User selectUserById(int userId);

    /**
     * 用户资料修改
     * @param user
     */
    @Update("update user  set sex=#{sex} ,email = #{email} ,Phone = #{phone} ,city=#{city},birthday=#{birthday} where id=#{userId}")
    @Results(id="UserResultMap" ,value = {
            @Result(property = "userId",column = "id"),
    })
    void updateUser(User user);

    /**
     * 用户头像修改
     * @param user
     */
    @Update("update user  set avatar=#{avatar} where id=#{userId}")
    @Results(id="UserResultMap" ,value = {
            @Result(property = "userId",column = "id"),
    })
    void updateUserAvatar(User user);


    /**
     * 添加用户图片中间表
     * @param imageIds
     */
    void addUserImage(Integer userId,@Param("list") List<Integer> imageIds);





}
