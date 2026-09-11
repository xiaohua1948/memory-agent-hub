package com.memory.xiaohua.mq;

import com.memory.xiaohua.mq.message.FileAnalysisMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 文件异步分析事件生产者。
 *
 * <p>上传主流程落库后调用，按分析类型投递到 {@link MqConstants#TOPIC_FILE} 的不同 tag，
 * 由逆地理编码 / 图像分类识别 / 人脸检测三个独立消费者组分别消费。</p>
 *
 * <p>发送失败仅记录日志、不向上抛出：分析属于上传的"增强能力"，
 * 不能因为 MQ 抖动导致已经落库成功的上传接口失败；
 * 遗漏的任务由定时补偿任务（CronSyncTask）或接口实时兜底处理。</p>
 */
@Slf4j
@Component
public class FileAnalysisProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public FileAnalysisProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    /** 投递逆地理编码事件（经纬度 → 地址） */
    public void sendGeocode(FileAnalysisMessage message) {
        send(MqConstants.TAG_FILE_GEOCODE, message);
    }

    /** 投递图像分类识别事件（缩略图 → AI 标签） */
    public void sendImageTag(FileAnalysisMessage message) {
        send(MqConstants.TAG_FILE_IMAGE_TAG, message);
    }

    /** 投递人脸检测事件（缩略图 → 人脸特征） */
    public void sendFace(FileAnalysisMessage message) {
        send(MqConstants.TAG_FILE_FACE, message);
    }

    private void send(String tag, FileAnalysisMessage message) {
        try {
            // topic 与 tag 用 "topic:tag" 形式拼接
            rocketMQTemplate.syncSend(
                    MqConstants.TOPIC_FILE + ":" + tag,
                    MessageBuilder.withPayload(message).build());
            log.info("[MQ] 投递文件分析事件 tag={}, fileId={}", tag, message.getFileId());
        } catch (Exception e) {
            log.error("[MQ] 投递文件分析事件失败 tag={}, fileId={}，将由定时任务兜底",
                    tag, message.getFileId(), e);
        }
    }
}
