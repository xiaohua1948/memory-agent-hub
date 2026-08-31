package com.memory.xiaohua.service;

import com.memory.xiaohua.model.dto.upload.UploadChunkVO;
import com.memory.xiaohua.model.dto.upload.UploadCompleteRequest;
import com.memory.xiaohua.model.dto.upload.UploadCompleteVO;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckRequest;
import com.memory.xiaohua.model.dto.upload.UploadPreCheckVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传服务
 *
 * <p>标准三阶段流程：<br>
 * 1. {@link #preCheck} 预检 —— 秒传判定 / 创建上传任务 / 返回断点续传进度<br>
 * 2. {@link #uploadChunk} 传分片 —— 单片上传，可乱序、可重传<br>
 * 3. {@link #complete} 合并 —— 对象存储服务端合并 + 校验 + 落库</p>
 */
public interface UploadService {

    /**
     * 预检：秒传则直接完成，否则创建/复用上传任务并返回已上传分片
     */
    UploadPreCheckVO preCheck(Long userId, UploadPreCheckRequest request);

    /**
     * 上传单个分片
     *
     * @param chunkIndex 分片序号，从 0 开始
     */
    UploadChunkVO uploadChunk(Long userId, String uploadId, int chunkIndex, MultipartFile chunk);

    /**
     * 合并分片并完成入库
     */
    UploadCompleteVO complete(Long userId, UploadCompleteRequest request, HttpServletRequest httpRequest);

    /**
     * 取消上传，清理对象存储上的残留分片
     */
    void abort(Long userId, String uploadId);

    /**
     * 清理超过有效期的未完成任务，返回清理数量
     */
    int cleanExpiredTasks();
}
