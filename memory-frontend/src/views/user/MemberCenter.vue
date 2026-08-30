<template>
    <div id="member">
        <div class="member-container">

            <!-- 会员状态展示 -->
            <div class="member-status">
                <el-card shadow="hover">
                    <div class="current-VIP">


                        <i-tdesign:user-vip class="VIP-icon" :style="{ color: isMember ? '#ffd700' : '#999' }" />


                        <span class="status-text">
                            {{ isMember ? `尊贵的${currentLevel}会员` : '您尚未开通会员' }}
                        </span>
                        <span v-if="isMember" class="expire-time">有效期至：{{ expireDate }}</span>
                    </div>
                </el-card>
            </div>

            <!-- 会员套餐选择 -->
            <div class="package-select">
                <el-radio-group v-model="selectedDuration" class="duration-switch">
                    <el-radio-button value="month">按月订阅</el-radio-button>
                    <el-radio-button value="year">按年订阅（立省30%）</el-radio-button>
                </el-radio-group>

                <div class="package-list">
                    <el-card v-for="(pkg, index) in packages" :key="index"
                        :class="['package-card', { 'active': selectedPackage === pkg.level }]"
                        @click="selectedPackage = pkg.level">
                        <div class="package-header">
                            <h3>{{ pkg.level }}会员</h3>
                            <el-tag v-if="pkg.recommend" type="danger" effect="dark">推荐</el-tag>
                        </div>

                        <div class="price-section">
                            <span class="currency">¥</span>
                            <span class="amount">{{ selectedDuration === 'month' ? pkg.monthPrice : pkg.yearPrice
                                }}</span>
                            <span class="duration">/{{ durationMap[selectedDuration] }}</span>
                        </div>

                        <div class="privilege-list">
                            <div v-for="(item, idx) in pkg.privileges" :key="idx" class="privilege-item">
                                <el-icon>
                                    <Check />
                                </el-icon>
                                <span>{{ item }}</span>
                            </div>
                        </div>

                        <el-button style="position: absolute;bottom: 10px;right:20px" type="primary"
                            :class="{ 'is-member': isMember && currentLevel === pkg.level }"
                            @click.stop="handleUpgrade(pkg)">
                            {{ getButtonText(pkg) }}
                        </el-button>
                    </el-card>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { Check } from '@element-plus/icons-vue'
import { getUserInfo } from '@/api/user/index'
import { storage } from '@/utils/storage';
import { handleUpdateUserStatus } from '@/api/user/index'
interface MemberPackage {
    level: 'VIP' | 'SVIP'
    monthPrice: number
    yearPrice: number
    privileges: string[]
    recommend?: boolean
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
// 会员套餐数据
const packages = ref<MemberPackage[]>([
    {
        level: 'VIP',
        monthPrice: 30,
        yearPrice: 300,
        privileges: [
            '20GB 云存储空间',
            '高清图片上传',
            '专属客服支持',
            '优先审核通道'
        ]
    },
    {
        level: 'SVIP',
        monthPrice: 50,
        yearPrice: 500,
        privileges: [
            '50GB 云存储空间',
            '4K超清上传',
            'VIP专属模板',
            '双重备份保障',
            '24小时专属客服'
        ],
        recommend: true
    }
])

const durationMap = { month: '月', year: '年' } as const
// 类型推导为：{ readonly month: "月"; readonly year: "年"; }
const selectedDuration = ref<'month' | 'year'>('month')
// 修改计算属性，使用后端返回的 accountStatus 和 membershipDays
const isMember = computed(() => userInfo.value.accountStatus !== 'normal')
const currentLevel = computed(() => userInfo.value.accountStatus.toUpperCase()) // 适配 SVIP 大写显示
const expireDate = computed(() => {
    // 根据 membershipDays 计算到期日
    const date = new Date(userInfo.value.createTime)
    date.setDate(date.getDate() + userInfo.value.membershipDays)
    return date.toISOString().split('T')[0] // 格式化为 YYYY-MM-DD
})

// 套餐选择逻辑调整
const selectedPackage = ref<MemberPackage['level']>(
    userInfo.value.accountStatus === 'svip' ? 'SVIP' : 'VIP'
)



// 修改支付处理逻辑
const handleUpgrade = (pkg: MemberPackage) => {
    if (userInfo.value.accountStatus === pkg.level.toLowerCase()) return

    ElMessageBox.confirm(
        `升级到${pkg.level}会员将获得：${pkg.level === 'SVIP' ? '50GB存储' : '20GB存储'
        }，确认支付？`,
        '套餐确认',
        {
            // ...弹窗配置保持不变...
        }
    ).then(() => {
        userInfo.value.accountStatus = pkg.level.toLowerCase() as any
        // 这里应调用实际支付接口
      let day = selectedDuration.value === 'month' ? 30 : 365
        handleUpdateUserStatus({
            status: pkg.level.toLowerCase(),
            day: day,
        }) // 支付成功后更新状态
        userInfo.value.membershipDays += day
        ElMessage.success(`${pkg.level}会员开通成功！`)
    })
}

// 修改按钮文字逻辑
const getButtonText = (pkg: MemberPackage) => {
    const currentLevel = userInfo.value.accountStatus.toUpperCase()
    if (currentLevel === pkg.level) return '当前套餐'
    return userInfo.value.accountStatus !== 'normal' ? '立即切换' : '立即开通'
}
</script>

<style scoped>
#member {
    width: calc(100% - 40px);
    padding: 20px;
    height: calc(100% - 40px);
    position: relative;
}

.member-container {

    width: 100%;
    margin: 0 auto;
    overflow-y: auto;
}

.current-VIP {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 15px;
    padding: 20px;
}

.VIP-icon {
    font-size: 32px;
}

.status-text {
    font-size: 24px;
    font-weight: 500;
}

.expire-time {
    margin-left: auto;
    color: #666;
}





.duration-switch {
    margin-top: 10px;
    margin-bottom: 30px;
    display: flex;
    justify-content: center;
}





.package-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 30px;

}

.package-card {
    transition: all 0.3s;
    border: 2px solid #e0e0e0;
    cursor: pointer;
    position: relative;
    width: 300px;
    height: 400px;
}

.package-card.active {
    border-color: #409eff;
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.2);
}

.package-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.package-header h3 {
    font-size: 24px;
    margin: 0;
    padding: 0;
}

.price-section {
    text-align: center;

}

.currency {
    font-size: 24px;
    vertical-align: super;
}

.amount {
    font-size: 24px;
    font-weight: bold;
    margin: 0 5px;
}

.duration {
    color: #666;
    font-size: 16px;
}

.privilege-list {
    margin-top: 10px;
}

.privilege-item {
    display: flex;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #eee;
}

.privilege-item:last-child {
    border: none;
}

.privilege-item .el-icon {
    color: #67c23a;
    margin-right: 10px;
}

.el-button.is-member {
    background: #e0e0e0;
    border-color: #dcdcdc;
    color: #666;
    cursor: not-allowed;
}

@media (max-width: 768px) {
    .package-list {
        grid-template-columns: 1fr;
    }
}
</style>