package com.memory.xiaohua.service;

import com.memory.xiaohua.model.entity.Face;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 人像特征表 服务类
 * </p>
 *
 * @author xiaohua
 * @since 2025-03-07
 */
public interface FaceService extends IService<Face> {

    void processPictureFaceInfo();

    /**
     * 按文件异步处理人脸：查出该文件未处理的人脸记录并提取特征 / 聚类。
     * 供 MQ 消费者调用，与定时任务共用同一处理逻辑。
     *
     * @param fileId 文件主表 id
     * @param userId 文件归属用户 id
     */
    void processFaceByFileId(String fileId, Long userId);




}
