package com.memory.xiaohua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.AiSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI 会话 Mapper
 *
 * @author xiaohua
 */
public interface AiSessionMapper extends BaseMapper<AiSession> {

    /** 查询用户会话列表（按更新时间倒序） */
    @Select("select * from ai_session where user_id = #{userId} and deleted = 0 order by update_time desc")
    List<AiSession> selectByUserId(@Param("userId") Long userId);

    /** 按会话业务ID查询（校验归属用） */
    @Select("select * from ai_session where session_id = #{sessionId} and deleted = 0")
    AiSession selectBySessionId(@Param("sessionId") String sessionId);

    /** 更新会话标题（异步标题生成时使用） */
    @Update("update ai_session set title = #{title}, update_time = now() where session_id = #{sessionId} and user_id = #{userId} and deleted = 0")
    int updateTitle(@Param("sessionId") String sessionId, @Param("userId") Long userId, @Param("title") String title);

    /** 刷新会话更新时间（每轮 chat 后调用，避免覆盖标题） */
    @Update("update ai_session set update_time = now() where session_id = #{sessionId} and user_id = #{userId} and deleted = 0")
    int touch(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    /** 软删会话（deleted=1），返回受影响行数（0 表示会话不存在或无权限） */
    @Update("update ai_session set deleted = 1, update_time = now() where session_id = #{sessionId} and user_id = #{userId} and deleted = 0")
    int markDeleted(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
