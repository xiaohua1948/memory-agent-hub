package com.memory.xiaohua.model.dto;

import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025-04-25,00:31
 */
@Data
public class PageRequest {
    private Integer current;
    private Integer pageSize;

}