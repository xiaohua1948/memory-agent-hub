package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.entity.Record;
import com.memory.xiaohua.service.RecordService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 操作记录表 前端控制器
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-06
 */
@RestController
@RequestMapping("/record")
public class RecordController {
    @Resource
    private RecordService recordService;
    @PostMapping("/selectAll")
    public BaseResponse<?> RecoverPicture(Integer current ,Integer size) {

        Page<Record> page = new Page<>(current, size);
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        QueryWrapper<Record> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);

        Page<Record> recordPage = recordService.getBaseMapper().selectPage(page, queryWrapper);
        return ResultUtil.success(recordPage,"获取数据成功!") ;
    }

}
