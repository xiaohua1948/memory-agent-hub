<template>
    <div id="globalHeader">
        <div class="header-content">
            <div class="header-logo" @click="router.push('/home')">
                <img class="header-logo-image" src="../assets/image/logo/logo.png" alt="Logo">
                <span class="header-logo-text">Memory Space</span>
            </div>
            <el-input v-if="showSearh" v-model="input" placeholder="搜索图片" :suffix-icon="Search" class="search-input"
                @focus="router.push('/search')" />

            <div class="user-info" v-if="isLogin && userInfo">
                <el-avatar class="user-avatar" :src="userInfo.avatarUrl" />

                <el-dropdown trigger="hover" popper-class="user-dropdown">
                    <div class="user-info-main">
                        <span class="username">{{ userInfo.userName }}</span>
                        <el-icon class="dropdown-icon">
                            <arrow-down />
                        </el-icon>
                    </div>

                    <template #dropdown>
                        <div class="user-card">
                            <div class="user-meta">
                                <div class="user-role">
                                    <el-tag :type="getTagType(userInfo.accountStatus)" size="small"
                                        :effect="getTagEffect(userInfo.accountStatus)">
                                        {{ getUserLevelText(userInfo.accountStatus) }}
                                    </el-tag>
                                    <el-button v-if="userInfo.accountStatus === 'normal'" type="primary" size="small"
                                        class="upgrade-btn" @click="navigateToMember">
                                        升级会员
                                    </el-button>
                                </div>

                                <div class="storage-info">
                                    <div class="storage-label">
                                        <span>已用 {{ formatFileSize(userInfo.usedSpace) }} / 总 {{
                                            formatFileSize(userInfo.totalSpace) }}</span>
                                        <el-tooltip content="清理空间" placement="top">
                                            <el-icon class="clean-icon"><i-ep-brush /></el-icon>
                                        </el-tooltip>
                                    </div>
                                    <el-progress :percentage="storagePercentage" :color="customColors"
                                        :show-text="false" />
                                </div>
                            </div>

                            <el-dropdown-menu class="user-menu">
                                <el-dropdown-item class="menu-item" @click="goToUserInfo">
                                    <el-icon><i-ep-user /></el-icon>
                                    <span>用户中心</span>
                                </el-dropdown-item>
                                <el-dropdown-item class="menu-item" @click="router.push('/storageSet')">
                                    <el-icon><i-ep-folder /></el-icon>
                                    <span>空间管理</span>
                                </el-dropdown-item>
                                <el-dropdown-item class="menu-item" divided @click="handleLogout">
                                    <el-icon><i-ep-switch-button /></el-icon>
                                    <span>退出登录</span>
                                </el-dropdown-item>
                            </el-dropdown-menu>
                        </div>
                    </template>
                </el-dropdown>
            </div>
            <div class="user-info" v-else>
                <el-button class="login-btn" @click="goToLogin">登录</el-button>
                <el-button type="primary" class="register-btn" @click="goToRegister">注册</el-button>
            </div>



        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { accountLogout } from '@/api/user/auth'
import { getUserInfo } from '@/api/user/index'
import $bus from '@/utils/bus.ts'
import { storage, STORAGE_KEYS } from '@/utils/storage';
interface Props {
    showSearh: boolean
}
const props = defineProps<Props>()

const isLogin = ref(false);
const router = useRouter()
const input = ref('')

const customColors = [
    { color: '#3d7eff', percentage: 20 },
    { color: '#6d9eff', percentage: 40 },
    { color: '#9dbfff', percentage: 60 },
    { color: '#cedfff', percentage: 80 },
    { color: '#e6f0ff', percentage: 100 }
]

const userInfo = ref<API.UserInfo | null>(null)
const storagePercentage = computed(() =>
    userInfo.value ? Math.round((userInfo.value.usedSpace / userInfo.value.totalSpace)) : 0
)
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
    const formattedSize = size.toFixed(1).replace(/\.0$/, '')

    return `${formattedSize} ${units[unitIndex]}`
}



const goToLogin = () => {
    $bus.emit('loginForm', true);
}
const goToRegister = () => {
    $bus.emit('registerForm', true);

}

onMounted(() => {
    checkLogin();
    $bus.off('login')
    $bus.on('login', () => {
        checkLogin();
    })
})
const checkLogin = async () => {
    const token = localStorage.getItem('token');
    if (token) {
        const res = await getUserInfo();
        if (res.code == 200) {
            storage.set(STORAGE_KEYS.USER_INFO, res.data);
            userInfo.value = res.data;
            isLogin.value = true;
        }
    }
    else {
        isLogin.value = false;
    }
}
const handleLogout = async () => {
    isLogin.value = false;
    try {

        const resp = await accountLogout();
        if (resp.code == 200) {

            ElMessage({
                type: 'success',
                message: resp.message
            });

        }
        else {
            ElMessage({
                type: 'error',
                message: resp.message
            });
        }

    }
    catch (error) {
    }
    finally {
        setTimeout(() => {
            router.push('/login');
        }, 500);
        localStorage.removeItem(STORAGE_KEYS.TOKEN);
        storage.remove(STORAGE_KEYS.USER_INFO);
    }
}
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
const navigateToMember = () => {
    router.push({ name: 'memberCenter' })
}

const getTagEffect = (level: string) => {
    return level === 'normal' ? 'plain' : 'dark'
}
const goToUserInfo = () => {
    router.push({ name: 'userInfo' });
}
</script>

<style scoped>
#globalHeader {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 10;
    width: 100%;
    height: 60px;
    background: #ffffff;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.header-content {
    display: flex;
    align-items: center;
    flex-direction: row;
    height: 100%;
    margin: 0 auto;
    padding: 0 30px 0px 10px;
}

.header-logo {

    width: 220px;

    height: 60px;
    display: flex;
    align-items: center;
    margin-left: 10px;
    margin-right: 10px;
    user-select: none;
    cursor: pointer;
    transition: transform 0.2s ease;
}

.header-logo-image {
    height: 50px;
    margin-right: 5px;
    transition: transform 0.2s ease;
}

.header-logo-text {
    white-space: nowrap;
    font-family: 'Montserrat', 'Roboto', sans-serif;
    /* 科技感无衬线字体 */
    font-weight: 400;
    /* 保持视觉重量 */
    font-size: clamp(2rem, 5vw, 3.5rem);
    /* 响应式字体大小 */
    letter-spacing: -0.05em;
    /* 紧凑字间距增强整体感 */
    font-size: 24px;
    background: linear-gradient(135deg,
            #4A90E0 20%,
            /* 主蓝 */
            #6B8CFF 50%,
            /* 过渡蓝紫 */
            #FFA86B 80%
            /* 琥珀橙点缀 */
        );
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    text-shadow:
        1px 1px 2px rgba(74, 144, 224, 0.3),
        /* 浅蓝外发光 */
        -1px -1px 2px rgba(255, 168, 107, 0.2);
    /* 暖色反光 */

    /* 动态交互 */
    transition: all 0.3s ease;
    display: inline-block;
    /* 确保动画效果生效 */
}

.header-logo:hover {
    background-position: 100% 50%;
    /* 渐变色流动 */
    cursor: pointer;
    transform: scale(1.05);
}


.search-input {
    max-width: 200px;
    min-width: 75px;
    transition: all 0.3s ease;
}



.user-info {
    display: flex;
    align-items: center;
    margin-left: auto;
    gap: 2px;
}

.user-avatar {
    width: 40px;

    transition: transform 0.2s ease;
    border-radius: 50%;
}

.user-avatar:hover {
    transform: scale(1.1);
}

.user-info-main {
    display: flex;
    align-items: center;
    cursor: pointer;
}

.user-info-main:hover {
    background: rgba(61, 126, 255, 0.05);
}

.username {
    font-weight: 500;
    color: #333;
    margin-right: 5px;
    white-space: nowrap;
}

.dropdown-icon {
    color: #666;
    transition: transform 0.3s ease;
}

.user-info-main:hover .dropdown-icon {
    color: #3d7eff;
}


.login-btn {
    width: 90px;
    border-radius: 20px;


}

.register-btn {
    width: 90px;
    border-radius: 20px;
    background: #3d7eff;
}

/* 下拉菜单样式 */

:deep(.el-dropdown-menu) {
    width: 200px;
    /* 自定义宽度 */

}

.user-card {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 5px;

}

.user-meta {
    padding: 8px;
}

.user-role {
    display: flex;
    align-items: center;
    gap: 12px;
    justify-content: center;
    margin-bottom: 16px;
}

.upgrade-btn {
    padding: 6px 16px !important;
    border-radius: 20px !important;
}

.storage-info {
    position: relative;
}

.storage-label {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 12px;
    color: #666;
}

.clean-icon {
    cursor: pointer;
    color: #999;
    transition: color 0.3s ease;
}

.clean-icon:hover {
    color: #3d7eff;
}

:deep(.el-progress-bar) {
    border-radius: 4px;
}

:deep(.el-progress-bar__outer) {
    background-color: #f5f7fa;
}



.user-menu {
    border: none !important;
    padding: 0 !important;
}

.menu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 16px !important;
    border-radius: 8px !important;
    transition: all 0.3s ease !important;
}

.menu-item i {
    font-size: 16px;
}

.menu-item:hover {
    background: rgba(61, 126, 255, 0.05) !important;
    color: #3d7eff !important;
}

.menu-item:hover i {
    color: #3d7eff !important;
}
</style>
