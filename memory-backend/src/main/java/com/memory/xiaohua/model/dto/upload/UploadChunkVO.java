package com.memory.xiaohua.model.dto.upload;

import lombok.Data;

/**
 * 分片上传响应
 */
@Data
public class UploadChunkVO {

    /** 本次上传的分片序号 */
    private int chunkIndex;

    /** 服务端已收到的分片数 */
    private int uploadedCount;

    /** 分片总数 */
    private int totalChunks;

    /** 是否已收齐所有分片（true 时前端应立即调用 complete） */
    private boolean allChunksDone;
}
