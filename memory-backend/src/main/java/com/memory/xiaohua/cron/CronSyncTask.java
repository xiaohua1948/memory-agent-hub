package com.memory.xiaohua.cron;

import com.memory.xiaohua.service.FaceService;
import com.memory.xiaohua.service.FileService;
import com.memory.xiaohua.service.LocationService;
import com.memory.xiaohua.service.RecycleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/2,16:10
 */
@Component
@Slf4j
public class CronSyncTask {

    @Resource
    private RecycleService recycleService;

    @Resource
    private LocationService locationService;

    @Resource
    private FaceService faceService;

    @Resource
    private FileService fileService;

    /*
       服务启动后立即执行一次
       */
    @EventListener(ApplicationReadyEvent.class)
    public void LocationOnStartup() {
        log.info("应用启动完成，触发首次地理位置同步");

        locationService.cronLocationFromCoordinates();
    }
    /**
     * 定时任务（每50分钟执行）
     */
    @Scheduled(cron = "0 0/50 * * * ?")
    @Transactional
    public void CronLocation() {
        log.info("定时任务触发地理位置同步");
        locationService.cronLocationFromCoordinates();
    }


    @EventListener(ApplicationReadyEvent.class)
    public void DropPictureOnStartUp(){
        // log.info("应用启动完成，触发删除回收站照片");
       // recycleService.cronDropPicture();
    }
    //每天凌晨执行
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void CronDropPictureSync() {
        log.info("定时任务触发删除回收站照片");
        recycleService.cronDropPicture();
    }


    @EventListener(ApplicationReadyEvent.class)
    public void CronUpdateFileUrlOnStartUp(){
        //log.info("应用启动完成，触发修改文件Url");
        fileService.cronUpdateFileUrl();
    }
    @Scheduled(cron = "0 0 0 */6 * ?")
    public void  CronUpdateFileUrl(){
        fileService.cronUpdateFileUrl();
    }


    @EventListener(ApplicationReadyEvent.class)
    public void FaceOnStartUp(){
        // log.info("应用启动完成，触发首次人脸信息同步");
       //faceService.processPictureFaceInfo();
    }

    @Scheduled(cron = "0 0/50 * * * ?")
    @Transactional
    public void CronFaceSync() {
        log.info("定时任务触发同步照片人脸信息");
        faceService.processPictureFaceInfo();
    }







}