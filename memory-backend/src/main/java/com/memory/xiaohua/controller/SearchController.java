package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.visual.FileTagVO;
import com.memory.xiaohua.model.vo.visual.VisualLocationVO;
import com.memory.xiaohua.service.AlbumService;
import com.memory.xiaohua.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/10,18:06
 */
@RestController
@RequestMapping("/search")
public class SearchController {
    @Resource
    private FileService fileService;
    @Resource
    private AlbumService albumService;

    @GetMapping("/selectAllModels")
    public BaseResponse<?> selectAllLocation(){
        Long userId = Long.parseLong((String) StpUtil.getLoginId());
        List<String> res = fileService.selectAllModels(userId);
        return ResultUtil.success(res,"获取数据成功!");
    }

    @PostMapping("/selectFileListByKeyword")
    @Operation(summary = "获取文件信息", description = "分页按照排序字段、类型、图片类型获取文件信息")
    public BaseResponse<?> selectFileListByKeyWord(Integer current, Integer size, String orderType, String orderKeyword, String imageTypeText, String searchType, String searchKeyword) {
        // 分组时间
        if ("upload_time".equals(orderKeyword)) {
            orderKeyword = "upload_time";
        } else {
            orderKeyword = "date_time_original";
        }
        // 排序方式
        if ("desc".equals(orderType)) {
            orderType = "desc";
        } else {
            orderType = "asc";
        }
        if (current == null || size == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }
        //图片类型
        Set<String> imageTypeTextSet = new HashSet<>(Arrays.asList("picture", "gif", "video"));
        imageTypeText = imageTypeTextSet.contains(imageTypeText) ? imageTypeText : "all";
        Page<FileInfoListVO> res = new Page<>();
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        res = switch (searchType) {
            case "tag" ->
                    fileService.getTagFileInfo(current, size, userId, orderType, orderKeyword, imageTypeText, searchKeyword);
            case "model" ->
                    albumService.getModelFileInfo(current, size, userId, orderType, orderKeyword, imageTypeText, null, searchKeyword);
            case "location" ->
                    fileService.getFileInfoList(current, size, orderType, orderKeyword, imageTypeText, "city", searchKeyword, userId, null, false);
            default -> res;
        };
        return ResultUtil.success(res,"获取数据成功");
    }
}
