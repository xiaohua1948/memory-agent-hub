package com.memory.xiaohua.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.model.vo.picture.TagResult;
import com.memory.xiaohua.mq.message.FileAnalysisMessage;
import com.memory.xiaohua.utils.RedisKeyConstants;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import com.memory.xiaohua.utils.picture.ImageTagCacheUtil;
import com.memory.xiaohua.utils.picture.ImageTagUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图像分类识别消费者：把"调华为云识别标签"从请求线程剥离为异步事件。
 *
 * <p>上传完成后异步调用华为云识别，结果写入 Redis 缓存；
 * 前端识别接口命中缓存即秒回，未命中再实时识别兜底。</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.TOPIC_FILE,
        selectorExpression = MqConstants.TAG_FILE_IMAGE_TAG,
        consumerGroup = MqConstants.CONSUMER_GROUP_FILE_IMAGE_TAG,
        messageModel = MessageModel.CLUSTERING)
public class FileImageTagConsumer implements RocketMQListener<MessageExt> {

    private static final long DEDUP_TTL_HOURS = 24L;

    private final ObjectMapper objectMapper;
    private final MinioOSSUtil minioOSSUtil;
    private final ImageTagUtil imageTagUtil;
    private final ImageTagCacheUtil imageTagCacheUtil;
    private final RedisUtil redisUtil;

    public FileImageTagConsumer(ObjectMapper objectMapper,
                                MinioOSSUtil minioOSSUtil,
                                ImageTagUtil imageTagUtil,
                                ImageTagCacheUtil imageTagCacheUtil,
                                RedisUtil redisUtil) {
        this.objectMapper = objectMapper;
        this.minioOSSUtil = minioOSSUtil;
        this.imageTagUtil = imageTagUtil;
        this.imageTagCacheUtil = imageTagCacheUtil;
        this.redisUtil = redisUtil;
    }

    @Override
    public void onMessage(MessageExt message) {
        FileAnalysisMessage msg;
        try {
            msg = objectMapper.readValue(message.getBody(), FileAnalysisMessage.class);
        } catch (Exception e) {
            log.error("[MQ] 图像分类识别消息反序列化失败，msgId={}，直接丢弃", message.getMsgId(), e);
            return;
        }
        if (msg.getFileId() == null) {
            return;
        }
        if (!markIfFirst(msg.getFileId())) {
            log.info("[MQ] 图像分类识别消息重复，跳过 fileId={}", msg.getFileId());
            return;
        }
        try {
            process(msg);
        } catch (Exception e) {
            redisUtil.delete(dedupKey(msg.getFileId()));
            log.error("[MQ] 图像分类识别失败 fileId={}，将触发重试/死信", msg.getFileId(), e);
            throw new RuntimeException(e);
        }
    }

    private void process(FileAnalysisMessage msg) {
        String thumbnailObjectName = msg.getThumbnailObjectName();
        if (thumbnailObjectName == null) {
            log.info("[MQ] 文件无缩略图，跳过图像分类识别 fileId={}", msg.getFileId());
            return;
        }
        byte[] fileBytes = minioOSSUtil.getFileBytes(thumbnailObjectName);
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("[MQ] 缩略图内容为空，跳过图像分类识别 fileId={}", msg.getFileId());
            return;
        }
        String base64String = Base64.getEncoder().encodeToString(fileBytes);
        List<TagResult> tagResult = imageTagUtil.classifyImage("base64", base64String);
        // 结果写入缓存，供识别接口直接命中
        imageTagCacheUtil.save(msg.getFileId(), tagResult);
        log.info("[MQ] 图像分类识别完成 fileId={}, 标签数={}", msg.getFileId(),
                tagResult == null ? 0 : tagResult.size());
    }

    private boolean markIfFirst(String fileId) {
        return redisUtil.setIfAbsent(dedupKey(fileId), "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
    }

    private String dedupKey(String fileId) {
        return RedisKeyConstants.MQ_DEDUP + MqConstants.TAG_FILE_IMAGE_TAG + ":" + fileId;
    }
}
