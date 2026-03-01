package com.memory.xiaohua.service;

import com.memory.xiaohua.model.entity.FileEntity;
import org.springframework.stereotype.Service;

@Service
public interface LocationService {
    void cronLocationFromCoordinates();
    void processFileWithDelay(FileEntity file);
}
