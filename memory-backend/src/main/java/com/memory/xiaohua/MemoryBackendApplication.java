package com.memory.xiaohua;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.memory.xiaohua.mapper")
@SpringBootApplication
@EnableScheduling
public class MemoryBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemoryBackendApplication.class, args);
    }

}
