package com.memory.xiaohua.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.entity.AiPendingAction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * AI 待确认操作 Mapper
 *
 * @author xiaohua
 */
public interface AiPendingActionMapper extends BaseMapper<AiPendingAction> {

    /** 更新操作状态（状态机迁移，带旧状态乐观校验防并发重复执行） */
    @Update("update ai_pending_action set status = #{toStatus}, confirm_time = now() " +
            "where action_id = #{actionId} and user_id = #{userId} and status = #{fromStatus}")
    int updateStatus(@Param("actionId") String actionId,
                     @Param("userId") Long userId,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);
}
