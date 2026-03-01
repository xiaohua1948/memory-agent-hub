<!-- AccountCenter.vue -->
<template>
  <div class="account-container">
    <!-- 左侧导航 -->
    <div class="sidebar">
      <!-- 用户信息展示 -->
      <div class="user-profile">
        <div class="avatar-wrapper" @click="showUploadDialog">
          <el-image :src="userInfo.avatarUrl" class="avatar-image" fit="cover" />
          <div class="edit-overlay">
            <el-icon>
              <EditPen />
            </el-icon>
          </div>
        </div>
        <div class="profile-info">
          <div class="user-name">{{ userInfo.userName }}</div>
          <div class="user-status">
            <el-tag :type="getTagType(userInfo.accountStatus)" size="small" :effect="getTagEffect(userInfo.accountStatus)">
              {{ getUserLevelText(userInfo.accountStatus) }}
            </el-tag>
            <el-button v-if="userInfo.accountStatus === 'normal'" type="primary" size="small" class="upgrade-btn"
              @click="navigateToMember">
              升级会员
            </el-button>
          </div>
        </div>
      </div>

      <el-menu class="side-menu" router :default-active="currentRouteName" active-text-color="rgb(99,125,255)"
        :collapse="false">
        <el-menu-item index="userInfo" :route="{ name: 'userInfo' }">
          <el-icon class="custom-icon">
            <User />
          </el-icon>
          <span>基本信息</span>
        </el-menu-item>

        <el-menu-item index="securitySet" :route="{ name: 'securitySet' }">
          <el-icon class="custom-icon">
            <Lock />
          </el-icon>
          <span>安全设置</span>
        </el-menu-item>

        <el-menu-item index="storageSet" :route="{ name: 'storageSet' }">
          <el-icon class="custom-icon">
            <Files />
          </el-icon>
          <span>容量管理</span>
        </el-menu-item>

        <el-menu-item index="memberCenter" :route="{ name: 'memberCenter' }">
          <el-icon class="custom-icon">
            <Sell />
          </el-icon>
          <span>会员中心</span>
        </el-menu-item>
      </el-menu>
    </div>

    <!-- 右侧内容 -->
    <div class="main-content">
      <router-view />
    </div>

    <!-- 头像上传对话框 -->
    <el-dialog v-model="uploadVisible" class="avatar-dialog" title="更换头像" width="400" :append-to-body="true">

      <el-upload :headers="headers" :limit="1" ref="uploadRef" class="avatar-uploader" :auto-upload="false"
        :action="uploadUrl" :show-file-list="false" drag :on-success="handleAvatarSuccess"
        :on-change="checkAvatarUpload" :on-exceed="handleExceed">
        <el-image v-if="tempAvatar" :src="tempAvatar" class="upload-avatar" fit="cover" />
        <el-icon v-else class="upload-icon">
          <Plus />
        </el-icon>
      </el-upload>

      <template #footer>
        <span class="upload-tip">tip:点击或者拖拽图片上传</span>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAvatar">确认更换</el-button>
      </template>
    </el-dialog>
  </div>



</template>

<script setup lang="ts">

import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Files, EditPen, Plus } from '@element-plus/icons-vue'
import type { UploadInstance, UploadProps, UploadRawFile } from 'element-plus'
import { genFileId } from 'element-plus'
import { useRoute } from 'vue-router'
import requestPublicConfig from '@/api/config'
import { getUserInfo } from '@/api/user/index'
import { storage } from '@/utils/storage';
const route = useRoute()
const uploadRef = ref<UploadInstance>()
const currentRouteName = route.name as string

const userInfo = ref(<API.UserInfo>{
    userId: 1000000001,
    userName: "小华同学",
    account: "xiaohua",
    email: "1609048001@qq.com",
    createTime: "2025-02-20T15:18:26",
    updateTime: "2025-03-04T22:31:21",
    profile: "小华爱编程",
    avatarUrl: "http://127.0.0.1:9000/pictures//avatar/1000000001/1000000001.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=nbOVDrriWWUzwqzoeSuW%2F20250304%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250304T143121Z&X-Amz-Expires=604800&X-Amz-SignedHeaders=host&X-Amz-Signature=45944478663dcbcd0dc86e95a91312ee598384d012d4872cdad548f35fd64b8a",
    totalSpace: 21474836480,
    usedSpace: 32988616,
    accountStatus: "normal",
    membershipDays: 100000
})
const token = localStorage.getItem('token');
onMounted(() => {
  const Info = storage.get<API.UserInfo>('USER_INFO');
  if (Info) {
    userInfo.value = Info;
  }
  else {
    selectUserInfo();
  }
})
const selectUserInfo = async () => {
  const token = localStorage.getItem('token');
  if (token) {
    const res = await getUserInfo();
    if (res.code == 200) {
      userInfo.value = res.data;

    }
  }
}
const headers = computed(() => ({
  Authorization: token,
}));


const uploadUrl = requestPublicConfig.baseUrl + '/user/uploadAvatar'
const uploadVisible = ref(false)
// 临时存储上传的头像
const tempAvatar = ref('')

const showUploadDialog = () => {
  tempAvatar.value = ''
  uploadVisible.value = true
}


const handleExceed: UploadProps['onExceed'] = (files) => {
  uploadRef.value!.clearFiles()
  const file = files[0] as UploadRawFile
  file.uid = genFileId()
  uploadRef.value!.handleStart(file)
}

const handleAvatarSuccess: UploadProps['onSuccess'] = (res, file) => {
  tempAvatar.value = URL.createObjectURL(file.raw!)
  userInfo.value.avatarUrl = URL.createObjectURL(file.raw!)
  storage.set('USER_INFO', userInfo.value);
}

const checkAvatarUpload: UploadProps['onChange'] = (file) => {
  tempAvatar.value = URL.createObjectURL(file.raw!)
  if (file.raw?.type !== 'image/jpeg') {
    ElMessage.error('Avatar picture must be JPG format!')
    return false
  } else if (file.raw.size / 1024 / 1024 > 2) {
    ElMessage.error('Avatar picture size can not exceed 2MB!')
    return false
  }
  return true
}
// tempAvatar.value = URL.createObjectURL(file)
// 验证逻辑保持不变


const confirmAvatar = () => {
  uploadRef.value!.submit()
  if (tempAvatar.value) {
    uploadRef.value!.submit()
    userInfo.value.avatarUrl = tempAvatar.value
    uploadVisible.value = false
    ElMessage.success('头像更新成功')
  } else {
    ElMessage.warning('请先选择要上传的头像')
  }
}

const router = useRouter()
const navigateToMember = () => {
  router.push({ name: 'memberCenter' })
}

// 用户等级相关方法
const getUserLevelText = (level: string) => {
  const levelMap: Record<string, string> = {
    normal: '普通用户',
    vip: 'VIP用户',
    svip: '超级VIP用户'
  }
  return levelMap[level] || '未知身份'
}

type ValidTagType = "success" | "warning" | "info" | "primary" | "danger"
const getTagType = (level: string): ValidTagType => {
  const typeMap: Record<string, ValidTagType> = {
    normal: 'info',
    vip: 'danger',
    svip: 'warning'
  }
  return typeMap[level] || 'info'
}

const getTagEffect = (level: string) => {
  return level === 'normal' ? 'plain' : 'dark'
}
</script>

<style scoped>
.account-container {
  position: relative;
  margin: 20px auto;
  border-radius: 8px;
  box-shadow: 4px 4px 4px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  height: 800px;
  transition: 0.5s ease-in-out;
}




/* 侧边栏 */
.sidebar {
  max-width: 200px;
  background: #fff;
  border-right: #dcdfe6 solid 1px;
}

/* 用户简介 */
.user-profile {
  text-align: center;
  margin-top: 20px;
  margin-bottom: 20px;
  height: 150px;
}

/* 菜单 */
.side-menu {
  width: 200px;
  height: calc(100% - 190px);
  border: 0;
}

.side-menu .el-menu-item {
  width: 100%;
  display: flex;
  justify-content: center;
}

.main-content {
  width: calc(100% - 200px);
  margin-top: 10px;
  height: calc(100% - 10px);
}

@media screen and (max-height: 1280px) {
  .account-container {
    height: 800px;
  }
}

@media screen and (max-height: 1080px) {
  .account-container {
    height: 800px;
  }
}

@media screen and (max-height: 868px) {
  .account-container {
    height: 630px;
  }
}




@media screen and (max-width: 2048px) {
  .account-container {
    max-width: 1500px;

  }
}

/* 当屏幕宽度小于等于 1920px 时应用的样式 */
@media screen and (max-width: 1920px) {
  .account-container {
    max-width: 1000px;

  }
}

/* 当屏幕宽度小于等于 1366px 时应用的样式 */
@media screen and (max-width: 1280px) {
  .account-container {
    max-width: 700px;

  }
}

/* 当屏幕宽度小于等于 1366px 时应用的样式 */
@media screen and (max-width: 1280px) {
  .side-menu {
    width: 150px;
  }

  .main-content {
    width: calc(100% - 150px);
  }
}

/* 当屏幕宽度小于等于 1366px 时应用的样式 */
@media screen and (max-width: 868px) {
  .side-menu {
    width: 100px;
  }

  .main-content {
    width: calc(100% - 100px);
  }
}



.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  margin: 0 auto 15px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s;
}

.avatar-image {
  width: 100%;
  height: 100%;
}

.avatar-wrapper:hover {
  transform: scale(1.05);
}

.avatar-wrapper:hover .edit-overlay {
  opacity: 1;
}

/* 头像hover */
.edit-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.edit-overlay .el-icon {
  color: #fff;
  font-size: 20px;
}

.profile-info {
  margin-top: 10px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.user-status {
  display: flex;
  gap: 10px;
  justify-content: center;
  justify-items: center;
}

.upgrade-btn {
  height: 20px;

}


/* 更换头像 */
.avatar-dialog {
  width: 30%;
  border-radius: 20%;
  position: relative;
}

.avatar-uploader {
  display: flex;
  justify-content: center;
  align-items: center;

  /* cursor: pointer;
  border: 1px dashed #d9d9d9; */
}

:deep(.el-upload-dragger) {
  width: 200px;
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0;
  padding: 0;
  border-radius: 50%;
}

upload-avatar {
  width: 100%;
  height: 100%;
}


.upload-icon {
  font-size: 28px;
  color: #8c939d;

}



/* 自定义激活状态样式 */
:deep(.el-menu-item.is-active) {
  background-color: rgba(99, 125, 255, 0.1) !important;
  color: rgb(99, 125, 255) !important;
}

:deep(.el-menu-item:hover) {
  background-color: rgba(37, 61, 180, 0.05);
}

.upload-tip {
  display: block;
  /* 使提示信息独占一行 */

  padding-top: 8px;
  /* 内边距 */
  padding-bottom: 8px;
  /* 内边距 */
  color: red;
  /* 文字颜色 */
  border-radius: 4px;
  /* 圆角 */
  font-size: 14px;
  /* 字体大小 */
  text-align: center;
  /* 文字居中 */

}
</style>
