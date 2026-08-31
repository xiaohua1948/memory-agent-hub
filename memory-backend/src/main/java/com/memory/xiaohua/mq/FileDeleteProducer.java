package com.memory.xiaohua.mq;

import com.memory.xiaohua.mq.message.FileDeleteMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 文件彻底删除消息生产者。
 * 发送至 {@link MqConstants#TOPIC_FILE} topic，tag={@link MqConstants#TAG_FILE_DELETE}，
 * 由 FileDeleteConsumer 异步消费，完成 OSS 对象/ file 主表/ 秒传缓存的彻底清理。
 */
@Slf4j
@Component
public class FileDeleteProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public FileDeleteProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void send(FileDeleteMessage message) {
        // topic 与 tag 用 "topic:tag" 形式拼接
        rocketMQTemplate.syncSend(
                MqConstants.TOPIC_FILE + ":" + MqConstants.TAG_FILE_DELETE,
                MessageBuilder.withPayload(message).build());
        log.info("[MQ] 投递文件彻底删除消息 fileId={}, md5={}", message.getFileId(), message.getMd5());
    }
}
