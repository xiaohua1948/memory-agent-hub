package com.memory.xiaohua.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.exception.BusinessException;
import com.memory.xiaohua.exception.StatusCode;
import com.memory.xiaohua.model.dto.DownLoadInfoDTO;
import com.memory.xiaohua.model.entity.Album;
import com.memory.xiaohua.model.vo.FileInfoListVO;
import com.memory.xiaohua.model.vo.album.AlbumVO;
import com.memory.xiaohua.model.vo.album.LocationAlbumVO;
import com.memory.xiaohua.model.vo.album.ModelAlbumVO;
import com.memory.xiaohua.service.AlbumService;
import com.memory.xiaohua.service.FileService;
import com.memory.xiaohua.service.RecordService;
import com.memory.xiaohua.utils.auth.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * <p>
 * 相册信息表 前端控制器
 * </p>
 *
 * @author xiaohua
 * @since 2025-02-24
 */
@RestController
@RequestMapping("/album")
public class AlbumController {
    @Resource
    private AlbumService albumService;
    @Resource
    private FileService fileService;
    @Resource
    private RecordService recordService;

    @Resource
    private RedisUtil redisUtil;

    @PostMapping("/create")
    @Operation(summary = "创建相册", description = "创建相册")
    public BaseResponse<?> addAlbum(HttpServletRequest request,String albumName) {

        if (albumName == null || albumName.length() > 30 || albumName.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "相册名称错误！");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Album album = new Album();
        album.setUserId(userId);
        album.setAlbumName(albumName);
        albumService.getBaseMapper().insert(album);

        //日志记录HttpServletRequest request
        recordService.createRecordLog("创建相册:"+albumName,1,userId,request);
        return ResultUtil.success("\"" + albumName + "\"创建成功！");
    }

    @PostMapping("/selectAllAlbumInfo")
    @Operation(summary = "查询所有相册", description = "查询所有相册信息")
    public BaseResponse<Page<AlbumVO>> selectAllAlbum(String current, String size, String orderKeyword, String orderType) {
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }
        // 分组时间
        if ("update_time".equals(orderKeyword)) {
            orderKeyword = "update_time";
        } else {
            orderKeyword = "create_time";
        }
        // 排序方式
        if ("desc".equals(orderType)) {
            orderType = "desc";
        } else {
            orderType = "asc";
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());

        Page<AlbumVO> albumVOPage = albumService.selectAllAlbum(c, s, orderKeyword, orderType, userId);

        return ResultUtil.success(albumVOPage, "获取数据成功！");
    }

    @PostMapping("/selectAllLocationAlbum")
    @Operation(summary = "查询所有地点相册", description = "查询所有按地点分组的相册")
    public BaseResponse<Page<LocationAlbumVO>> selectAllLocationAlbum(String current, String size, String locationLevel) {
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }
        // 分组时间
        Set<String> set = new HashSet<>();
        set.add("country");
        set.add("province");
        set.add("city");
        set.add("district");
        if(!set.contains(locationLevel)){
            //默认分组
            locationLevel = "city";
        }

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<LocationAlbumVO> locationAlbumVOPage = albumService.selectAllLocationAlbum(c, s, locationLevel, userId);
        return ResultUtil.success(locationAlbumVOPage, "获取数据成功！");
    }
    @PostMapping("/selectAllModelAlbum")
    @Operation(summary = "查询所有地点相册", description = "查询所有按地点分组的相册")
    public BaseResponse<?> selectAllModelAlbum(String current, String size) {
        int c;
        int s;
        try {
            c = Integer.parseInt(current);
            s = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分页错误!");
        }


        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Page<ModelAlbumVO> modelAlbumVOPage = albumService.selectAllModelAlbum(c, s, userId);
        return ResultUtil.success(modelAlbumVOPage, "获取数据成功！");
    }


    @PostMapping("/selectModelAlbumFileInfo")
    @Operation(summary = "获取文件信息", description = "分页按照排序字段、类型、图片类型获取文件信息")
    public BaseResponse<?> SelectFileList(String current, String size, String orderType, String orderKeyword, String imageTypeText,String makeName, String modelName) {

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
        Page<FileInfoListVO> modelFileInfo = albumService.getModelFileInfo(c, s, userId,orderType, orderKeyword, imageTypeText, makeName, modelName);

        return ResultUtil.success(modelFileInfo,"获取文件信息成功");
    }



    @PostMapping("/selectAlbumById")
    @Operation(summary = "查询相册信息", description = "根据Id查询相册信息")
    public BaseResponse<AlbumVO> selectAlbumById(Long albumId) {
        if (albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        AlbumVO albumVO = albumService.selectAlbumById(albumId, userId);
        return ResultUtil.success(albumVO, "获取数据成功！");
    }


    @PostMapping("/addPictureToAlbum")
    @Operation(summary = "添加图片至相册", description = "将选中的图片加入相册")
    public BaseResponse<?> addPictureToAlbum(HttpServletRequest request, @RequestParam List<String> fileIds, Long albumId) {
        if (fileIds == null || albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        albumService.addPictureToAlbum(fileIds, albumId, userId);
        //日志记录HttpServletRequest request
        recordService.createRecordLog("添加照片到相册",fileIds.size(),userId,request);
        return ResultUtil.success("添加成功！");
    }

    @PostMapping("/removePictureFromAlbum")
    @Operation(summary = "从相册移除图片", description = "将选中的图片移除相册")
    public BaseResponse<?> removePictureFromAlbum(HttpServletRequest request,@RequestParam List<String> fileIds, Long albumId) {
        if (fileIds == null || albumId == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        albumService.removePictureFromAlbum(fileIds, albumId, userId);
        //日志记录HttpServletRequest request
        recordService.createRecordLog("移出相册",fileIds.size(),userId,request);
        return ResultUtil.success("移除成功！");
    }

    @PostMapping("/updateAlbumInfo")
    @Operation(summary = "修改相册信息", description = "修改相册的信息")
    public BaseResponse<?> UpdateAlbumInfo(HttpServletRequest request,@RequestBody Album album) {
        //参数校验
        String albumName = album.getAlbumName();
        String description = album.getDescription();

        if (albumName == null || description == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        if (albumName.length() > 30 || description.length() > 80) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Boolean res = albumService.updateAlbumInfo(album, userId);
        if(res) {
            //日志记录HttpServletRequest request
            recordService.createRecordLog("修改相册信息:"+albumName,1,userId,request);
        }
        return res ? ResultUtil.success("修改成功！") : ResultUtil.error(StatusCode.PARAMS_ERROR, "修改失败！");
    }

    @PostMapping("/updateAlbumCover")
    @Operation(summary = "修改相册封面", description = "将选中的照片id作为相册封面")
    public BaseResponse<?> UpdateAlbumCover(HttpServletRequest request,String fileId, Long albumId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Boolean res = albumService.updateAlbumCover(albumId, fileId, userId);
        if(res) {
            //日志记录HttpServletRequest request
            recordService.createRecordLog("修改相册封面",1,userId,request);
        }
        return res ? ResultUtil.success("修改成功！") : ResultUtil.error(StatusCode.PARAMS_ERROR, "修改失败！");
    }

    @PostMapping("/deleteAlbum")
    @Operation(summary = "删除相册", description = "删除相册，是否删除相册里的图片")
    public BaseResponse<?> deleteAlbum(HttpServletRequest request,@RequestParam ArrayList<Long> albumIds, Boolean isDeletePicture) {

        if (CollectionUtils.isEmpty(albumIds) || isDeletePicture == null) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        Boolean res = albumService.deleteAlbum(albumIds, isDeletePicture, userId);

        if(res) {
            //日志记录HttpServletRequest request
            recordService.createRecordLog("删除相册",albumIds.size(),userId,request);
        }
        return res ? ResultUtil.success("删除成功！") : ResultUtil.error(StatusCode.PARAMS_ERROR, "删除失败！");
    }

    @PostMapping("/getDownloadAlbumToken")
    @Operation(summary = "下载相册", description = "下载相册")
    public BaseResponse<?> getDownloadAlbumToken(Long albumId) {

        if (albumId == null ) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }

        Long userId = Long.parseLong(StpUtil.getLoginId().toString());

        String downloadAlbumToken = fileService.getDownloadAlbumToken(albumId, userId);
        return ResultUtil.success(downloadAlbumToken,"获取下载token成功");
    }
    @GetMapping("/downloadAlbumByToken")
    public void downloadAlbumByToken(HttpServletRequest request,HttpServletResponse response, String downloadToken) {

        DownLoadInfoDTO downLoadInfoDTO = (DownLoadInfoDTO) redisUtil.get("tempDownload:"+downloadToken);

        if (downloadToken == null){
            throw new BusinessException(StatusCode.PARAMS_ERROR, "令牌已过期!");
        }
        Long albumId = downLoadInfoDTO.getAlbumId();
        Long userId = downLoadInfoDTO.getUserId();
        fileService.downloadAlbumByToken(request,response,albumId,userId);
        redisUtil.delete("tempDownload:"+downloadToken);
    }



    @PostMapping("/createShareAlbumUrl")
    public BaseResponse<?> createShareAlbumUrl(HttpServletRequest request,Integer shareDay,@RequestParam List<Long> albumIds) {

        if (albumIds == null || albumIds.isEmpty()) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "参数错误!");
        }
        if (shareDay == null || shareDay <= 0) {
            throw new BusinessException(StatusCode.PARAMS_ERROR, "分享天数必须大于0");
        }
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        String shareKey = fileService.createAlbumShareUrl(albumIds, userId, shareDay);

        //日志记录
        recordService.createRecordLog("分享相册:"+shareDay+"天",albumIds.size(),userId,request);
        // 返回分享链接（这里返回短标识，前端自己拼链接）
        return ResultUtil.success(shareKey,"分享成功！");
    }




}
