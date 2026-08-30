package com.memory.xiaohua.service;

import com.memory.xiaohua.model.entity.Record;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作记录表 服务类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-06
 */
public interface RecordService extends IService<Record> {

    void createRecordLog(String operation,  Integer number,  Long userId, HttpServletRequest request);

}
