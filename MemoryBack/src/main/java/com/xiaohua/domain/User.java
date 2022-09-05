package com.xiaohua.domain;

import com.alibaba.fastjson.annotation.JSONField;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
/**
 * @program: MemoryBack
 * @description 用户实体类
 * @author: xiaohua
 * @create: 2022-09-02 14:00
 **/
public class User {

    private Integer userId;
    private String userName;
    private String passWord;
    private String sex;
    private String email;
    private String phone;
    private String city;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date birthday;
    private Integer capacity;
    private String avatar;

    public User(Integer userId, String userName, String passWord, String sex, String email, String phone, String city, Date birthday, Integer capacity, String avatar) {
        this.userId = userId;
        this.userName = userName;
        this.passWord = passWord;
        this.sex = sex;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.birthday = birthday;
        this.capacity = capacity;
        this.avatar = avatar;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", passWord='" + passWord + '\'' +
                ", sex='" + sex + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", birthday=" + birthday +
                ", capacity=" + capacity +
                ", avatar='" + avatar + '\'' +
                '}';
    }



    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public User(String userName, String passWord) {
        this.setUserName(userName);
        this.setPassWord(passWord);
    }


    public User(){
    }

}
