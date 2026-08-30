<template>
    <div class="profile-container">
      <el-row :gutter="20">
        <el-col :span="6" :xs="24">
          <el-card class="profile-card">
            <div slot="header" class="profile-header">
              <span>个人信息</span>
            </div>
            <div class="profile-content">
              <div class="text-center">
                <userAvatar />
              </div>
              <ul class="profile-list profile-striped-list">
                <li class="profile-list-item">
                  <svg-icon icon-class="user" />用户名称
                  <div class="profile-right">{{ user.userName }}</div>
                </li>
                <li class="profile-list-item">
                  <svg-icon icon-class="phone" />手机号码
                  <div class="profile-right">112341242342</div>
                </li>
                <li class="profile-list-item">
                  <svg-icon icon-class="email" />用户邮箱
                  <div class="profile-right">{{ user.email }}</div>
                </li>
                <li class="profile-list-item">
                  <svg-icon icon-class="tree" />所属部门
                  <div class="profile-right" >MemorySpace</div>
                </li>
                <li class="profile-list-item">
                  <svg-icon icon-class="peoples" />所属角色
                  <div class="profile-right">{{user.role}}</div>
                </li>
                <li class="profile-list-item">
                  <svg-icon icon-class="date" />创建日期
                  <div class="profile-right">{{ formattedCreateTime }}</div>
                </li>
              </ul>
            </div>
          </el-card>
        </el-col>
        <el-col :span="18" :xs="24">
          <el-card class="profile-detail-card">
            <div slot="header" class="profile-header">
              <span>基本资料</span>
            </div>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="基本资料" name="userinfo">
                <userInfo :user="user" />
              </el-tab-pane>
              <el-tab-pane label="修改密码" name="resetPwd">
                <resetPwd />
              </el-tab-pane>
            </el-tabs>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </template>
  

<script>
import userAvatar from "./userAvatar";
import userInfo from "./userInfo";
import resetPwd from "./resetPwd";
import {  getInfo } from '@/api/user'

export default {
  name: "Profile",
  components: { userAvatar, userInfo, resetPwd },
  data() {
    return {
      user: {},
      roleGroup: {},
      postGroup: {},
      activeTab: "userinfo"
    };
  },
  created() {
    this.getUser();
  },
  computed: {
    formattedCreateTime() {
      const date = new Date(this.user.createTime);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`;
    }
  },
  methods: {
    getUser() {
        getInfo().then(response => {
        this.user = response.data;
        this.roleGroup = response.roleGroup;
        this.postGroup = response.postGroup;
      });
    }
  }
};
</script>
<style scoped lang="scss">
.profile-container {
  padding: 20px;
}
.text-center{
    display: flex;
    justify-content: center;
}
.profile-card {
  margin-bottom: 20px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.profile-content {
  padding: 15px 0;
}

.profile-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.profile-striped-list {
  li:nth-child(odd) {
    background-color: #f9f9f9;
  }
}

.profile-list-item {
  display: flex;
  justify-content:left;
  align-items: center;
  padding: 12px 15px;
  border-bottom: 1px solid #eee;

  &:last-child {
    border-bottom: none;
  }
}

.profile-right {
    margin-left: auto;
  font-weight: 500;
}

.profile-detail-card {
  margin-top: 20px;
}
</style>