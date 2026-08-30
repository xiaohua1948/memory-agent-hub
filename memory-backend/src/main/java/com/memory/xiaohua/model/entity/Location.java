package com.memory.xiaohua.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/3/2,17:04
 */
@Data
@TableName("location")
public class Location {
    private String fileId;
    // 结构化地址信息
    private String country;
    private String province;  // 省
    private String city;      // 市


    private String district;      // 区
    private String township;  // 乡镇
    private String street;    // 街道
    private String streetNumber; // 门牌号
    private String fullAddress; // 完整地址
    public Location(){}
    public Location(String fileId, String country,String province, String city, String district, String township, String street, String streetNumber, String fullAddress) {
        this.fileId = fileId;
        this.country = country;
        this.province = province;
        this.city = city;
        this.district = district;
        this.township = township;
        this.street = street;
        this.streetNumber = streetNumber;
        this.fullAddress = fullAddress;
    }
}
