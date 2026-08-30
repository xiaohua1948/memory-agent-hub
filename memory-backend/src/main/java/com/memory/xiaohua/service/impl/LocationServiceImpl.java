package com.memory.xiaohua.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.memory.xiaohua.mapper.FileMapper;
import com.memory.xiaohua.mapper.LocationMapper;
import com.memory.xiaohua.model.entity.FileEntity;
import com.memory.xiaohua.model.entity.Location;
import com.memory.xiaohua.service.LocationService;
import com.memory.xiaohua.utils.picture.GeocoderLocationUtil;
import jakarta.annotation.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/7,22:38
 */
@Slf4j
@Service
public class LocationServiceImpl implements LocationService {
    @Resource
    private GeocoderLocationUtil geocoderUtil;

    // 在类中注入线程池执行器
    @Resource
    private TaskScheduler taskScheduler;
    @Resource
    FileMapper fileMapper;
    @Resource
    private LocationMapper locationMapper;


    public void cronLocationFromCoordinates() {
        List<FileEntity> files = fileMapper.findUnprocessedGeoData();
       
        // 按文件索引计算延迟时间（每个文件间隔1秒）
        for (int i = 0; i < files.size(); i++) {
            FileEntity file = files.get(i);
            long delay = i * 1000L; // 第n个文件延迟n秒
            taskScheduler.schedule(() -> {
              
                processFileWithDelay(file);
            }, new Date(System.currentTimeMillis() + delay));
        }
    }

    public void processFileWithDelay(FileEntity file) {
        try {
            GeocoderLocationUtil.Regeocode regeocode = geocoderUtil.reverseGeocode(
                    file.getLongitude(),
                    file.getLatitude()
            );
            if (regeocode != null && StringUtils.isNotBlank(regeocode.getFormattedAddress())) {

                GeocoderLocationUtil.AddressComponent a = regeocode.getAddressComponent();

                // 数据库操作保持原子性
                String number = "";
                String street = "";

                if (a.getStreetNumber() != null) {
                    number = a.getStreetNumber().getNumber();
                    street = a.getStreetNumber().getStreet();
                }

                //String fullAddress = a.getCountry()+a.getProvince()+a.getCity()+a.getDistrict()+a.getTownship()+street+number;
                Location location = new Location(file.getFileId(), a.getCountry(), a.getProvince(), a.getCity(), a.getDistrict(), a.getTownship(), street, number, regeocode.getFormattedAddress());
                locationMapper.insert(location);

                UpdateWrapper<FileEntity> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("file_id", file.getFileId())
                        .set("location", regeocode.getFormattedAddress());
                fileMapper.update(updateWrapper);
                log.info("更新文件 {} 的地址：{}", file.getFileId(), regeocode.getFormattedAddress());
            }
        } catch (GeocoderLocationUtil.GeocodingException e) {
            log.warn("地理编码失败: 文件ID={}", file.getFileId(), e);

        }
    }
}
