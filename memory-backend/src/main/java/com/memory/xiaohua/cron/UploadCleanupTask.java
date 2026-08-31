package com.memory.xiaohua.cron;

import com.memory.xiaohua.service.UploadService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 上传任务清理定时任务
 *
 * <p>分片上传被中断且前端未调用 abort 时，对象存储上会残留未完成的分片。
 * 这里定期回收超过有效期的任务，避免存储被无限占用。</p>
 */
@Slf4j
@Component
public class UploadCleanupTask {

    @Resource
    private UploadService uploadService;

    /**
     * 每天凌晨 3:30 清理超过有效期的未完成分片上传
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredUploadTasks() {
        try {
            int count = uploadService.cleanExpiredTasks();
            if (count > 0) {
                log.info("清理过期上传任务完成，共 {} 个", count);
            }
        } catch (Exception e) {
            log.error("清理过期上传任务失败", e);
        }
    }
}
