package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.service.RecordService;
import com.memory.xiaohua.service.RecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/5,19:40
 */
@RestController
@RequestMapping("/recycle")
@Tag(name = "回收站接口", description = "回收站接口")
public class RecycleController {
    @Resource
    private RecycleService recycleService;

    @Resource
    private RecordService recordService;

    @PostMapping("/selectAllFile")
    @Operation(summary = "获取回收站文件信息", description = "分页按照排序字段、类型、图片类型获取文件信息")
    public BaseResponse<Page<FileInfoListVO>> SelectFileList(String current, String size, String orderKeyword,String orderType, String imageTypeText) {

        // 分组时间
       orderKeyword = "deleted_time";
        // 排序方式
        if ("desc".equals(orderType)) {
            orderType = "desc";
        } else {
            orderType = "asc";
        }
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }

        //图片类型
        Set<String> imageTypeTextSet = new HashSet<>(Arrays.asList("picture", "gif", "video"));
        imageTypeText = imageTypeTextSet.contains(imageTypeText) ? imageTypeText : "all";
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<FileInfoListVO> fileInfoList = recycleService.getFileInfoList(c, s, orderType, orderKeyword, imageTypeText, userId);

        BaseResponse<Page<FileInfoListVO>> res = new BaseResponse<>();
        res.setData(fileInfoList);
        res.setCode(200);
        res.setMessage("获取文件信息成功!");
        return res;
    }

    @PostMapping("/recoverPicture")
    public BaseResponse<?> RecoverPicture(HttpServletRequest request, @RequestParam List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        recycleService.recoverPicture(fileIds,userId);
        //日志记录HttpServletRequest request
        recordService.createRecordLog("恢复图片",fileIds.size(),userId,request);
        return ResultUtil.success("恢复成功!") ;
    }


    @PostMapping("/dropPicture")
    @Operation(summary = "彻底删除", description = "彻底删除文件信息")
    public BaseResponse<?> dropPicture(HttpServletRequest request,@RequestParam List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        recycleService.dropPicture(fileIds,userId);

        //日志记录
        recordService.createRecordLog("恢复图片",fileIds.size(),userId,request);
        return ResultUtil.success("删除成功!") ;
    }






}
