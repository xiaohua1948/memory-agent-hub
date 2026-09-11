package com.memory.xiaohua.utils.picture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memory.xiaohua.model.vo.picture.TagResult;
import com.memory.xiaohua.utils.RedisKeyConstants;
import com.memory.xiaohua.utils.auth.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图像分类识别结果缓存。
 *
 * <p>上传后由 MQ 消费者异步调用华为云识别并把结果写入本缓存；
 * 前端调用识别接口时优先读缓存，命中即秒回，避免每次请求都同步调用第三方 API。
 * 缓存未命中（如异步任务尚未完成或已过期）时由接口实时识别并回填。</p>
 */
@Slf4j
@Component
public class ImageTagCacheUtil {

    /** 识别结果缓存有效期：3 天 */
    private static final long TTL_HOURS = 72L;

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public ImageTagCacheUtil(RedisUtil redisUtil, ObjectMapper objectMapper) {
        this.redisUtil = redisUtil;
        this.objectMapper = objectMapper;
    }

    /** 写入缓存（异步识别完成后调用） */
    public void save(String fileId, List<TagResult> tags) {
        if (fileId == null || tags == null) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(tags);
            redisUtil.set(RedisKeyConstants.FILE_IMAGE_TAG + fileId, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("[ImageTag] 识别结果写缓存失败 fileId={}", fileId, e);
        }
    }

    /** 读取缓存，未命中返回 null */
    public List<TagResult> get(String fileId) {
        if (fileId == null) {
            return null;
        }
        try {
            Object value = redisUtil.get(RedisKeyConstants.FILE_IMAGE_TAG + fileId);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(String.valueOf(value), new TypeReference<List<TagResult>>() {
            });
        } catch (Exception e) {
            log.warn("[ImageTag] 读取识别缓存失败 fileId={}", fileId, e);
            return null;
        }
    }
}
