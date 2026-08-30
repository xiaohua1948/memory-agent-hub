package com.memory.xiaohua.mapper;

import com.memory.xiaohua.model.entity.Record;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.memory.xiaohua.model.vo.visual.MemoryVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 操作记录表 Mapper 接口
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-06
 */
public interface RecordMapper extends BaseMapper<Record> {

    MemoryVO selectOperationStats(
            @Param("uploadKey") String uploadKey,
            @Param("downloadKey") String downloadKey
    );
}
