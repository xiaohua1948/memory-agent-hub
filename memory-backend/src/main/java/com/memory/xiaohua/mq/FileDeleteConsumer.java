package com.memory.xiaohua.mq;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.UserFileMapper;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.UserFileEntity;
import com.memory.xiaohua.mq.message.FileDeleteMessage;
import com.memory.xiaohua.utils.auth.RedisUtil;
import com.memory.xiaohua.utils.file.MinioOSSUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

/**
 * 文件彻底删除消费者。
 *
 * <p>逻辑：收到消息后判断该 file 主表是否还有人持有（user_file 仍有关联）。
 * <ul>
 *   <li>无人持有：物理删除 file 主表 + OSS 原图/缩略图 + 清理 Redis 秒传缓存；</li>
 *   <li>仍有人持有：仅清理 Redis 秒传缓存（保留 file 主表与 OSS 对象，供其他账号继续秒传）；</li>
 * </ul>
 *
 * <p>RocketMQ 重试机制：消费抛异常默认重试 {@code max-reconsume-times} 次，
 * 超过后消息自动进入 {@code %DLQ%+consumerGroup} 死信队列，便于排查（无需手动 ACK）。</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.TOPIC_FILE,
        selectorExpression = MqConstants.TAG_FILE_DELETE,
        consumerGroup = MqConstants.CONSUMER_GROUP_FILE_DELETE,
        messageModel = MessageModel.CLUSTERING)
public class FileDeleteConsumer implements RocketMQListener<MessageExt> {

    private final FileMapper fileMapper;
    private final UserFileMapper userFileMapper;
    private final MinioOSSUtil minioOSSUtil;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public FileDeleteConsumer(FileMapper fileMapper,
                              UserFileMapper userFileMapper,
                              MinioOSSUtil minioOSSUtil,
                              RedisUtil redisUtil,
                              ObjectMapper objectMapper) {
        this.fileMapper = fileMapper;
        this.userFileMapper = userFileMapper;
        this.minioOSSUtil = minioOSSUtil;
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MessageExt message) {
        FileDeleteMessage msg;
        try {
            msg = objectMapper.readValue(message.getBody(), FileDeleteMessage.class);
        } catch (Exception e) {
            log.error("[MQ] 文件删除消息反序列化失败，msgId={}，直接丢弃", message.getMsgId(), e);
            return;
        }
        try {
            process(msg);
        } catch (Exception e) {
            log.error("[MQ] 文件彻底删除处理失败 fileId={}，将触发 RocketMQ 重试/死信", msg.getFileId(), e);
            // 抛异常使 RocketMQ 按 max-reconsume-times 重试，最终进 %DLQ% 死信队列
            throw new RuntimeException(e);
        }
    }

    private void process(FileDeleteMessage msg) {
        String fileId = msg.getFileId();

        // 1. 判断是否还有人持有该文件（任意用户仍有关联）
        QueryWrapper<UserFileEntity> holderQuery = new QueryWrapper<>();
        holderQuery.eq("file_id", fileId);
        long holderCount = userFileMapper.selectCount(holderQuery);

        if (holderCount > 0) {
            // 仍有其他账号持有：仅清理秒传缓存，保留 file 主表与 OSS 对象供其复用
            clearMd5Cache(msg.getMd5());
            log.info("[MQ] 文件仍有 {} 人持有，仅清理秒传缓存 fileId={}", holderCount, fileId);
            return;
        }

        // 2. 无人持有：物理删除 file 主表 + OSS 对象 + 秒传缓存
        QueryWrapper<FileEntity> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("file_id", fileId);
        fileMapper.delete(deleteWrapper);

        if (msg.getObjectName() != null) {
            try {
                minioOSSUtil.delete(msg.getObjectName());
            } catch (Exception e) {
                log.warn("[MQ] 删除 OSS 原图失败（可能已不存在）objectName={}", msg.getObjectName(), e);
            }
        }
        if (msg.getThumbnailObjectName() != null) {
            try {
                minioOSSUtil.delete(msg.getThumbnailObjectName());
            } catch (Exception e) {
                log.warn("[MQ] 删除 OSS 缩略图失败（可能已不存在）objectName={}", msg.getThumbnailObjectName(), e);
            }
        }
        clearMd5Cache(msg.getMd5());
        log.info("[MQ] 文件无人持有，已彻底删除 fileId={}, md5={}", fileId, msg.getMd5());
    }

    private void clearMd5Cache(String md5) {
        if (md5 != null) {
            redisUtil.delete("upload:md5:" + md5);
        }
    }
}
