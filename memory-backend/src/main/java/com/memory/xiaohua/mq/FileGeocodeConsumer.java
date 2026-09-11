package com.memory.xiaohua.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.mq.message.FileAnalysisMessage;
import com.memory.xiaohua.service.LocationService;
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
 * 逆地理编码消费者：把"经纬度 → 地址"从上传主流程剥离为异步事件。
 *
 * <p>幂等保障：以 {@code mq:dedup:file-geocode:{fileId}} 作为 Redis 去重表，
 * 同一条消息重复投递时直接跳过；处理失败则删除去重标记并抛异常，
 * 交由 RocketMQ 按 max-reconsume-times 重试，最终进入死信队列。</p>
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqConstants.TOPIC_FILE,
        selectorExpression = MqConstants.TAG_FILE_GEOCODE,
        consumerGroup = MqConstants.CONSUMER_GROUP_FILE_GEOCODE,
        messageModel = MessageModel.CLUSTERING)
public class FileGeocodeConsumer implements RocketMQListener<MessageExt> {

    /** 去重标记有效期：24 小时（足够覆盖 RocketMQ 重试窗口） */
    private static final long DEDUP_TTL_HOURS = 24L;

    private final ObjectMapper objectMapper;
    private final FileMapper fileMapper;
    private final LocationService locationService;
    private final RedisUtil redisUtil;

    public FileGeocodeConsumer(ObjectMapper objectMapper,
                               FileMapper fileMapper,
                               LocationService locationService,
                               RedisUtil redisUtil) {
        this.objectMapper = objectMapper;
        this.fileMapper = fileMapper;
        this.locationService = locationService;
        this.redisUtil = redisUtil;
    }

    @Override
    public void onMessage(MessageExt message) {
        FileAnalysisMessage msg;
        try {
            msg = objectMapper.readValue(message.getBody(), FileAnalysisMessage.class);
        } catch (Exception e) {
            log.error("[MQ] 逆地理编码消息反序列化失败，msgId={}，直接丢弃", message.getMsgId(), e);
            return;
        }
        if (msg.getFileId() == null) {
            return;
        }
        // 幂等：同一条消息重复消费时直接跳过
        if (!markIfFirst(msg.getFileId())) {
            log.info("[MQ] 逆地理编码消息重复，跳过 fileId={}", msg.getFileId());
            return;
        }
        try {
            process(msg);
        } catch (Exception e) {
            // 处理失败：释放去重标记，保证重试时能被重新处理
            redisUtil.delete(dedupKey(msg.getFileId()));
            log.error("[MQ] 逆地理编码失败 fileId={}，将触发重试/死信", msg.getFileId(), e);
            throw new RuntimeException(e);
        }
    }

    private void process(FileAnalysisMessage msg) {
        FileEntity file = fileMapper.selectById(msg.getFileId());
        if (file == null) {
            log.warn("[MQ] 文件已不存在，跳过逆地理编码 fileId={}", msg.getFileId());
            return;
        }
        if (file.getLongitude() == null || file.getLatitude() == null) {
            log.info("[MQ] 文件无 GPS 坐标，跳过逆地理编码 fileId={}", msg.getFileId());
            return;
        }
        if (file.getLocation() != null) {
            log.info("[MQ] 文件地址已存在，跳过逆地理编码 fileId={}", msg.getFileId());
            return;
        }
        // 复用既有逆地理编码逻辑（内部已对高德 API 异常做容错处理）
        locationService.processFileWithDelay(file);
        log.info("[MQ] 逆地理编码完成 fileId={}", msg.getFileId());
    }

    private boolean markIfFirst(String fileId) {
        return redisUtil.setIfAbsent(dedupKey(fileId), "1", DEDUP_TTL_HOURS, TimeUnit.HOURS);
    }

    private String dedupKey(String fileId) {
        return RedisKeyConstants.MQ_DEDUP + MqConstants.TAG_FILE_GEOCODE + ":" + fileId;
    }
}
