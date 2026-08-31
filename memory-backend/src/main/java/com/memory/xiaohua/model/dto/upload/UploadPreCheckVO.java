package com.memory.xiaohua.model.dto.upload;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 上传预检响应
 */
@Data
public class UploadPreCheckVO {

    /** true 表示秒传成功，文件已入库，无需再传分片 */
    private boolean uploaded;

    /** 秒传成功时的文件 ID */
    private String fileId;

    /** 上传任务 ID，后续 chunk / complete 需携带 */
    private String uploadId;

    /** 服务端约定的分片大小（字节），前端必须按此切片 */
    private long chunkSize;

    /** 分片总数 */
    private int totalChunks;

    /** 已上传的分片序号（断点续传时前端需跳过这些分片） */
    private List<Integer> uploadedChunks;

    /** 任务过期时间戳（毫秒） */
    private long expiresAt;
}
