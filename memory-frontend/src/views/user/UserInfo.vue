<template>
  <div class="info-container">
    <el-form ref="formRef" :model="userInfo" label-width="100px" class="user-form">
      <el-form-item label="账号 ID">
        <el-input v-model="userInfo.userId" disabled />
      </el-form-item>

      <el-form-item label="昵称" prop="userName">
        <el-input v-model="userInfo.userName" />
      </el-form-item>

      <el-form-item label="注册邮箱">
        <el-input v-model="userInfo.email" disabled />
      </el-form-item>

      <el-form-item label="注册时间">
        <el-input :value="formatDate(userInfo.createTime)" disabled />
      </el-form-item>

      <el-form-item label="个人简介" prop="profile">
        <el-input v-model="userInfo.profile" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" :maxlength="200"
          show-word-limit resize='none' />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">保存修改</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type UploadProps } from 'element-plus'
import { updateUserInfo, getUserInfo } from '@/api/user/index'
import { storage } from '@/utils/storage';

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
  accountStatus: "svip",
  membershipDays: 100000
})
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

const nameRules = [
  { required: true, message: '请输入昵称', trigger: 'blur' },
  { min: 2, max: 20, message: '长度在2到20个字符', trigger: 'blur' }
]
const profileRules = [
  { max: 200, message: '个人简介不能超过200个字符', trigger: 'blur' }
]

// 用户等级显示文本
const getUserLevelText = (level: string) => {
  const levelMap: { [key: string]: string } = {
    normal: '普通用户',
    vip: 'VIP用户',
    svip: '超级VIP用户'
  }
  return levelMap[level] || '未知身份'
}

// 标签类型
type ValidTagType = "success" | "warning" | "info" | "primary" | "danger"

const getTagType = (level: string): ValidTagType => {
  const typeMap: Record<string, ValidTagType> = {
    normal: 'info',
    vip: 'danger',
    svip: 'warning'
  }
  return typeMap[level] || 'info'
}

// 标签效果
const getTagEffect = (level: string) => {
  return level === 'normal' ? 'plain' : 'dark'
}



const handleAvatarSuccess: UploadProps['onSuccess'] = (res, file) => {
  userInfo.value.avatarUrl = URL.createObjectURL(file.raw!)
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = ['image/jpeg', 'image/png'].includes(file.type)
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) ElMessage.error('只能上传JPG/PNG格式图片!')
  if (!isLt2M) ElMessage.error('图片大小不能超过2MB!')
  return isImage && isLt2M
}

const handleSubmit = async () => {


  const res = await updateUserInfo(userInfo.value);
  if (res.code == 200) {
    ElMessage.success('用户信息修改成功！')
    storage.set('USER_INFO', userInfo.value);
    return
  }
  else {
    ElMessage.error("用户信息修改失败！")
  }

}


const formatDate = (isoString: string) => {
  const date = new Date(isoString);

  // 年月日补零
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0'); // 03[6,8](@ref)
  const day = String(date.getDate()).padStart(2, '0');        // 04[6,8](@ref)

  // 时间补零与时段判断
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, '0'); // 02[6](@ref)
  const period = hours >= 12 ? '下午' : '上午';                // 时段[5](@ref)


  return `${year}年${month}月${day}日 ${period}${String(hours).padStart(2, '0')}:${minutes}`;

};

</script>

<style scoped>
.info-container {
  width: 100%;
  height: 100%;
}

.user-form {

  padding: 20px;
  padding-left: 0;
}
</style>