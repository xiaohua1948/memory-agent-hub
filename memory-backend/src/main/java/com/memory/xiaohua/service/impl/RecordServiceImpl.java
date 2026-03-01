package com.memory.xiaohua.service.impl;

import com.memory.xiaohua.model.entity.Record;
import com.memory.xiaohua.mapper.RecordMapper;
import com.memory.xiaohua.service.RecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.memory.xiaohua.utils.auth.IpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作记录表 服务实现类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-06
 */
@Service
public class RecordServiceImpl extends ServiceImpl<RecordMapper, Record> implements RecordService {
    @Resource
    IpUtil ipUtil;

    @Override
    public void createRecordLog(String operation,  Integer number, Long userId, HttpServletRequest request) {
        Record record = new Record();
        record.setNumber(number);
        record.setOperation(operation);
        record.setUserId(userId);
        record.setOperationTime(LocalDateTime.now());
        record.setIpv4(ipUtil.getClientIpAddress(request));
        this.save(record);
    }
}
