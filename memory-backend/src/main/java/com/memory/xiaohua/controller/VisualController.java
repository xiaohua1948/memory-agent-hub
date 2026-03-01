package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.LocationMapper;
import com.memory.xiaohua.mapper.PictureTagMapper;

import com.memory.xiaohua.model.vo.visual.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/5,23:27
 */
@RestController
@RequestMapping("/visual")
@Tag(name = "可视化接口", description = "可视化接口")
public class VisualController {
    @Resource
    PictureTagMapper pictureTagMapper;

    @Resource
    LocationMapper locationMapper;

    @Resource
    FileMapper fileMapper;


    @GetMapping("/selectAllTags")
    public BaseResponse<List<FileTagVO>> selectAllTags(){
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        List<FileTagVO> res = pictureTagMapper.selectAllTags(userId);
        return ResultUtil.success(res,"tag获取成功!");
    }


    @GetMapping("/selectAllLocations")
    public BaseResponse<?> selectAllLocation(){
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        List<VisualLocationVO> visualLocationVOS = locationMapper.selectAllLocation(userId);
        return ResultUtil.success(visualLocationVOS,"地理位置信息获取成功!");
    }

    @GetMapping("/selectAllFiles")
    public BaseResponse<?> selectAllFile(){
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        VisualFileVO visualFileVO = fileMapper.selectFileCount(userId);
        if(visualFileVO == null) {
            return ResultUtil.success(visualFileVO,"数据为空!");
        }
        List<FileContentType> fileContentTypes = fileMapper.selectVisualContentType(userId);
        List<FileSize> fileSizes = fileMapper.selectVisualFileSize(userId);
        visualFileVO.setSizeData(fileSizes);
        visualFileVO.setTypeData(fileContentTypes);
        return ResultUtil.success(visualFileVO,"地理位置信息获取成功!");
    }
}
