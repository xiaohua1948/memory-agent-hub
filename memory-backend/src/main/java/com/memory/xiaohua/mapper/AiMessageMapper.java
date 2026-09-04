package com.memory.xiaohua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.AiMessage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI 消息 Mapper
 *
 * @author xiaohua
 */
public interface AiMessageMapper extends BaseMapper<AiMessage> {

    /** 查询会话历史消息（按 id 升序，用于回灌记忆） */
    @Select("select * from ai_message where session_id = #{sessionId} and deleted = 0 order by id asc")
    List<AiMessage> selectBySessionId(@Param("sessionId") String sessionId);

    /** 查询会话最近 N 条消息（按 id 倒序取最近，记忆窗口回灌用） */
    @Select("select * from ai_message where session_id = #{sessionId} and deleted = 0 order by id desc limit #{limit}")
    List<AiMessage> selectRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /** 按会话软删全部消息（删除会话时使用） */
    @Update("update ai_message set deleted = 1 where session_id = #{sessionId} and deleted = 0")
    int markDeletedBySessionId(@Param("sessionId") String sessionId);
}
