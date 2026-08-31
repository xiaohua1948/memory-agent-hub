package com.memory.xiaohua.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.memory.xiaohua.common.BaseResponse;
import com.memory.xiaohua.common.ResultUtil;
import com.memory.xiaohua.model.dto.upload.UploadChunkVO;
import com.memory.xiaohua.model.dto.upload.UploadCompleteRequest;
import com.memory.xiaohua.model.dto.upload.UploadCompleteVO;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckRequest;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckVO;
import com.memory.xiaohua.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传接口
 *
 * <p>三阶段：pre-check（预检/秒传） → chunk（传分片，可并发可乱序） → complete（合并入库）。</p>
 */
@RestController
@RequestMapping("/file/upload")
public class UploadController {

    @Resource
    private UploadService uploadService;

    @Operation(summary = "上传预检", description = "秒传判定；未命中则创建上传任务并返回断点续传进度")
    @PostMapping("/pre-check")
    public BaseResponse<UploadPreCheckVO> preCheck(@RequestBody @Valid UploadPreCheckRequest request) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        return ResultUtil.success(uploadService.preCheck(userId, request), "预检完成");
    }

    @Operation(summary = "上传分片", description = "单个分片上传，支持并发、乱序与失败重传")
    @PostMapping(value = "/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<UploadChunkVO> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestPart("chunk") MultipartFile chunk) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        return ResultUtil.success(uploadService.uploadChunk(userId, uploadId, chunkIndex, chunk), "分片上传成功");
    }

    @Operation(summary = "合并分片", description = "校验完整性后由对象存储合并，解析元数据并入库")
    @PostMapping("/complete")
    public BaseResponse<UploadCompleteVO> complete(@RequestBody @Valid UploadCompleteRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        return ResultUtil.success(uploadService.complete(userId, request, httpRequest), "上传完成");
    }

    @Operation(summary = "取消上传", description = "放弃上传并清理对象存储上的残留分片")
    @DeleteMapping("/abort")
    public BaseResponse<?> abort(@RequestParam("uploadId") String uploadId) {
        Long userId = Long.parseLong(StpUtil.getLoginId().toString());
        uploadService.abort(userId, uploadId);
        return ResultUtil.success("已取消上传");
    }
}
