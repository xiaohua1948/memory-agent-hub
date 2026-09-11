package com.memory.xiaohua.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.mq.message.FileAnalysisMessage;
import com.memory.xiaohua.service.FaceService;
import com.memory.xiaohua.utils.RedisKeyConstants;
import com.memory.xiaohua.utils.auth.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 人脸检测消费者：把"OpenCV 人脸提取 + 人物聚类"从上传主流程剥离为异步事件。
 *
 * <p>处理逻辑复用 {@link FaceService#processFaceByFileId}，与定时任务
 * processPictureFaceInfo 共用同一套特征提取/聚类代码，此处只负责触发与幂等。</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.TOPIC_FILE,
        selectorExpression = MqConstants.TAG_FILE_FACE,
        consumerGroup = MqConstants.CONSUMER_GROUP_FILE_FACE,
        messageModel = MessageModel.CLUSTERING)
public class FileFaceConsumer implements RocketMQListener<MessageExt> {

    private static final long DEDUP_TTL_HOURS = 24L;

    private final ObjectMapper objectMapper;
    private final FaceService faceService;
    private final RedisUtil redisUtil;

    public FileFaceConsumer(ObjectMapper objectMapper,
                            FaceService faceService,
                            RedisUtil redisUtil) {
        this.objectMapper = objectMapper;
        this.faceService = faceService;
        this.redisUtil = redisUtil;
    }

    @Override
    public void onMessage(MessageExt message) {
        FileAnalysisMessage msg;
        try {
            msg = objectMapper.readValue(message.getBody(), FileAnalysisMessage.class);
        } catch (Exception e) {
            log.error("[MQ] 人脸检测消息反序列化失败，msgId={}，直接丢弃", message.getMsgId(), e);
            return;
        }
        if (msg.getFileId() == null) {
            return;
        }
        if (!markIfFirst(msg.getFileId())) {
            log.info("[MQ] 人脸检测消息重复，跳过 fileId={}", msg.getFileId());
            return;
        }
        try {
            faceService.processFaceByFileId(msg.getFileId(), msg.getUserId());
            log.info("[MQ] 人脸检测完成 fileId={}", msg.getFileId());
        } catch (Exception e) {
            redisUtil.delete(dedupKey(msg.getFileId()));
            log.error("[MQ] 人脸检测失败 fileId={}，将触发重试/死信", msg.getFileId(), e);
            throw new RuntimeException(e);
        }
    }

    private boolean markIfFirst(String fileId) {
        return redisUtil.setIfAbsent(dedupKey(fileId), "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
    }

    private String dedupKey(String fileId) {
        return RedisKeyConstants.MQ_DEDUP + MqConstants.TAG_FILE_FACE + ":" + fileId;
    }
}
