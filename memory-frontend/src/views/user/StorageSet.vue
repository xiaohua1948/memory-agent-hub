<template>
  <div class="capacity-container">
    <!-- 容量概览卡片 -->
    <el-card shadow="hover" class="capacity-card">
      <div class="capacity-header">
        <h2>存储容量管理</h2>
        <el-button type="primary" @click="showDialog = true">立即扩容</el-button>
      </div>

      <!-- 容量进度条 -->
      <el-progress :percentage="usagePercentage" :color="customColors" :stroke-width="16" :format="formatProgress" />

      <!-- 容量统计 -->
      <div class="capacity-stats">
        <div class="stat-item">
          <span class="stat-label">总容量</span>
          <span class="stat-value">{{ formatFileSize(userInfo.totalSpace) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">已使用</span>
          <span class="stat-value text-danger">{{ formatFileSize(userInfo.usedSpace) }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">剩余</span>
          <span class="stat-value text-success">{{ formatFileSize(remainingCapacity) }}</span>
        </div>
      </div>
    </el-card>

    <!-- 扩容对话框 -->
    <el-dialog v-model="showDialog" title="存储扩容" width="30%" :append-to-body="true">
      <el-form :model="form" label-width="80px">
        <el-form-item label="扩容大小" required>
          <el-input-number v-model="form.addCapacity" :min="1" :max="10" :step="1" controls-position="right" />
          <span class="unit">GB</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpgrade">确认扩容</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getUserInfo, handleAddUserStorage } from '@/api/user/index'

import { storage } from '@/utils/storage';
interface CapacityForm {
  addCapacity: number
}
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


const selectUserInfo = async () => {
  const token = localStorage.getItem('token');
  if (token) {
    const res = await getUserInfo();
    if (res.code == 200) {
      userInfo.value = res.data;

    }
  }
}
onMounted(() => {
  const Info = storage.get<API.UserInfo>('USER_INFO');
  if (Info) {
    userInfo.value = Info;
  }
  else {
    selectUserInfo();
  }
})
// 响应式数据

const showDialog = ref(false)
const form = ref<CapacityForm>({ addCapacity: 1 })

// 计算属性
const remainingCapacity = computed(() => userInfo.value.totalSpace - userInfo.value.usedSpace)
const usagePercentage = computed(() =>
  Math.round((userInfo.value.usedSpace / userInfo.value.totalSpace) * 100)
)

// 进度条颜色逻辑
const customColors = computed(() => [
  { color: '#67c23a', percentage: 20 },
  { color: '#e6a23c', percentage: 50 },
  { color: '#f56c6c', percentage: 80 }
])

// 方法定义
const formatProgress = () => `${usagePercentage.value}% 已使用`
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'

  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = Math.abs(bytes)
  let unitIndex = 0

  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }

  // 保留最多1位小数，并移除无用的末尾零
  const formattedSize = size.toFixed(5).replace(/\.0$/, '')

  return `${formattedSize} ${units[unitIndex]}`
}
const handleUpgrade = async () => {
  if (form.value.addCapacity <= 0) {
    ElMessage.error('请输入有效的扩容容量')
    return
  }

  userInfo.value.totalSpace += (form.value.addCapacity) * 1024 * 1024 * 1024;
  const res = await handleAddUserStorage({
    size: form.value.addCapacity * 1024 * 1024 * 1024
  })
  if (res.code == 200) {
    ElMessage.success(`成功扩容 ${formatFileSize(form.value.addCapacity * 1024 * 1024 * 1024)}, 新总容量: ${formatFileSize(userInfo.value.totalSpace)}`)

    return
  }
  else {
    ElMessage.error(`扩容失败`)
    return
  }

  showDialog.value = false
}
</script>

<style scoped>
.capacity-container {
  max-width: 800px;
  margin: 20px auto;
  padding: 20px;
}

.capacity-card {
  padding: 25px;
}

.capacity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.capacity-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 25px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-label {
  display: block;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
}

.text-danger {
  color: #f56c6c;
}

.text-success {
  color: #67c23a;
}

.unit {
  margin-left: 8px;
  color: #999;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .capacity-container {
    padding: 10px;
  }

  .capacity-stats {
    grid-template-columns: 1fr;
  }
}
</style>