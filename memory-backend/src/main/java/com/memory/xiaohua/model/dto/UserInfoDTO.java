package com.memory.xiaohua.model.dto;

import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/4,20:47
 */
@Data
public class UserInfoDTO {

    private String userName;

    private String profile;

    //    用户个人信息
    private Long userId;

    private Long totalSpace;

    private Long usedSpace;


    private String accountStatus;

    private Integer membershipDays;
}
