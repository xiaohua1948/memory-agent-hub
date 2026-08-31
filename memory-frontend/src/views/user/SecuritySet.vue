<template>
    <div class="security-container">

        <div class="security-items">
            <!-- 用户账号信息 -->
            <div class="security-item">
                <el-icon class="item-icon">
                    <User />
                </el-icon>
                <div class="item-content">
                    <h3>用户账号</h3>
                    <p class="account-info">{{ userInfo.account }}</p>
                </div>
                <el-button type="danger" class="action-btn" @click="showeAccountLogout">注销账号</el-button>
            </div>

            <!-- 修改密码 -->
            <div class="security-item">
                <el-icon class="item-icon">
                    <Lock />
                </el-icon>
                <div class="item-content">
                    <h3>修改密码</h3>
                    <p>定期修改密码保障账户安全</p>
                </div>
                <el-button type="primary" class="action-btn" @click="showPasswordDialog">修改密码</el-button>
            </div>

            <!-- 更换邮箱 -->
            <div class="security-item">
                <el-icon class="item-icon">
                    <Message />
                </el-icon>
                <div class="item-content">
                    <h3>更换邮箱</h3>
                    <p>当前绑定邮箱：{{ maskedEmail }}</p>
                </div>
                <el-button type="primary" class="action-btn" @click="showEmailDialog">更换邮箱</el-button>
            </div>
        </div>

        <!-- 新增注销账号对话框 -->
        <el-dialog v-model="logoutDialogVisible" title="注销账号" width="500px" :append-to-body="true">
            <el-form :model="logoutForm" :rules="emailRules" ref="logoutFormRef">
                <div class="warning-message">
                    <el-alert title="警告" type="error" :closable="false"
                        description="此操作将永久删除账户及所有数据，包括所有上传的图片记录，且不可恢复！" />
                </div>

                <el-form-item label="当前邮箱">
                    <el-input :model-value="maskedEmail" disabled />
                </el-form-item>

                <el-form-item label="当前密码" prop="password">
                    <el-input v-model="logoutForm.password" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>

                <el-form-item label="验证码" prop="code">
                    <div class="code-input">
                        <el-input v-model="logoutForm.code" placeholder="6位数字验证码" maxlength="6" />
                        <el-button :disabled="emailCodeTimer > 0" @click="sendEmailCode">
                            {{ emailCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>

            <template #footer>
                <el-button @click="logoutDialogVisible = false">取消</el-button>
                <el-button type="danger" @click="confirmLogout">确认注销</el-button>
            </template>
        </el-dialog>

        <!-- 修改密码对话框 -->
        <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px" :append-to-body="true">
            <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef">
                <el-form-item label="当前邮箱">
                    <el-input :model-value="maskedEmail" disabled />
                </el-form-item>
                <el-form-item label="原密码" prop="password">
                    <el-input v-model="passwordForm.password" type="password" show-password placeholder="请输入原密码" />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password
                        placeholder="6-20位字母数字组合" />
                </el-form-item>
                <!-- 新增确认密码表单项 -->
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password
                        placeholder="请再次输入新密码" />
                </el-form-item>

                <el-form-item label="验证码" prop="code">
                    <div class="code-input">
                        <el-input v-model="passwordForm.code" placeholder="6位数字验证码" maxlength="6" />
                        <el-button :disabled="passwordCodeTimer > 0" @click="sendPasswordCode">
                            {{ passwordCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="passwordDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
            </template>
        </el-dialog>

        <!-- 更换邮箱对话框 -->
        <el-dialog v-model="emailDialogVisible" title="更换邮箱" width="500px" :append-to-body="true">
            <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef">
                <el-form-item label="当前密码" prop="password">
                    <el-input v-model="emailForm.password" type="password" show-password placeholder="请输入当前密码" />
                </el-form-item>

                <el-form-item label="邮箱地址" prop="newEmail">
                    <el-input v-model="emailForm.newEmail" placeholder="请输入新的邮箱地址" />
                </el-form-item>

                <el-form-item label="验证码" prop="code">
                    <div class="code-input">
                        <el-input v-model="emailForm.code" placeholder="6位数字验证码" maxlength="6" />
                        <el-button :disabled="emailCodeTimer > 0" @click="sendEmailCode">
                            {{ emailCodeText }}
                        </el-button>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="emailDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleChangeEmail">确认更换</el-button>
            </template>
        </el-dialog>

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getUserInfo } from '@/api/user/index'
import { storage, STORAGE_KEYS } from '@/utils/storage';
import { updatePassWord, updateEmail, getEmailCode } from '@/api/user/auth'
// 新增响应式数据
const logoutDialogVisible = ref(false)
const logoutFormRef = ref<FormInstance>()
const logoutForm = reactive({
    password: '',
    code: ''
})
const router = useRouter()

const userInfo = ref<API.UserInfo | null>(null)


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
    const Info = storage.get<API.UserInfo>(STORAGE_KEYS.USER_INFO);
    if (Info) {
        userInfo.value = Info;
    }
    else {
        selectUserInfo();
    }
})
const maskedEmail = computed(() => {
    const [name, domain] = userInfo.value.email.split('@')
    return `${name.slice(0, 3)}****${name.slice(-2)}@${domain}`
})

// 修改密码相关
const passwordDialogVisible = ref(false)
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({
    newPassword: '',
    confirmPassword: '',
    code: '',
    password: '',
})

const passwordCodeTimer = ref(0)
const passwordCodeText = computed(() =>
    passwordCodeTimer.value > 0 ? `${passwordCodeTimer.value}秒后重发` : '获取验证码'
)

// 更换邮箱相关
const emailDialogVisible = ref(false)
const emailFormRef = ref<FormInstance>()
const originalEmail = ref('')
const emailForm = reactive({
    password: '',
    newEmail: '',
    code: ''
})

const emailCodeTimer = ref(0)
const emailCodeText = computed(() =>
    emailCodeTimer.value > 0 ? `${emailCodeTimer.value}秒后重发` : '获取验证码'
)

// 验证规则
const passwordRules: FormRules = {
    password: [
        { required: true, message: '请输入原密码', trigger: 'blur' }
    ],
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        {
            pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/,
            message: '6-20位字母数字组合', trigger: 'blur'
        }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value === passwordForm.newPassword) {
                    callback()
                } else {
                    callback(new Error('两次输入的密码不一致'))
                }
            },
            trigger: 'blur'
        }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
    ]
}

const emailRules: FormRules = {
    password: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
    ],
    newEmail: [
        { required: true, message: '请输入新邮箱', trigger: 'blur' },
        { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '验证码为6位数字', trigger: 'blur' }
    ]
}
const showeAccountLogout = () => {
    logoutForm.password = ''
    logoutForm.code = ''
    logoutDialogVisible.value = true
}


// 对话框控制
const showPasswordDialog = () => {
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordForm.code = ''
    passwordDialogVisible.value = true
}

const showEmailDialog = () => {
    emailForm.password = ''
    emailForm.newEmail = ''
    emailForm.code = ''
    emailDialogVisible.value = true
}

// 发送验证码
const sendPasswordCode = async () => {
    try {
        const res = await getEmailCode({
            email: userInfo.value.email,

        });
        if (res.code == 200) {
            startTimer('email')
            ElMessage.success('验证码已发送')
        }
        else {
            ElMessage.error('验证码发送失败')
        }
    } catch (error) {
        ElMessage.error('验证码发送失败')
    }
}

const sendEmailCode = async () => {
    try {
        // 调用发送验证码API
        const res = await getEmailCode({
            email: userInfo.value.email,

        });
        if (res.code == 200) {
            startTimer('email')
            ElMessage.success('验证码已发送')
        }
        else {
            ElMessage.error('验证码发送失败')
        }

    } catch (error) {
        ElMessage.error('验证码发送失败')
    }
}
const confirmLogout = async () => {

    await logoutFormRef.value?.validate()

    emailDialogVisible.value = false
    router.push('/login');


    ElMessage.success('账号注销成功')

}

// 处理提交
const handleChangePassword = async () => {
    try {
        await passwordFormRef.value?.validate()
        const res = await updatePassWord({
            password: passwordForm.password,
            confirmPassword: passwordForm.confirmPassword,
            newPassword: passwordForm.newPassword,
            email: userInfo.value.email,
            code: passwordForm.code
        })

        if (res.code == 200) {
            ElMessage.success('密码修改成功,3s后将自动退出登录');
            localStorage.removeItem('token')
            setTimeout(() => {
                router.push('/login')
            }, 3000)

        }
        else {
            ElMessage.error('密码更换失败')
        }
        passwordDialogVisible.value = false

    } catch (error) {
    }
}

const handleChangeEmail = async () => {
    try {
        await emailFormRef.value?.validate()
        const res = await updateEmail({
            password: emailForm.password,
            email: emailForm.newEmail,
            code: emailForm.code
        })
        if (res.code == 200) {
            originalEmail.value = emailForm.newEmail
            ElMessage.success('邮箱更换成功')

        }
        else {
            ElMessage.error('邮箱更换失败')
        }
        emailDialogVisible.value = false

    } catch (error) {
    }
}


// 倒计时逻辑
const startTimer = (type: 'password' | 'email') => {
    const timer = type === 'password' ? passwordCodeTimer : emailCodeTimer
    timer.value = 60

    const interval = setInterval(() => {
        if (timer.value > 0) {
            timer.value--
        } else {
            clearInterval(interval)
        }
    }, 1000)
}
</script>
<style scoped>
.security-container {
    height: 100%;
}


.security-items {
    display: grid;
    gap: 10px;
    padding: 20px;
}

.security-item {
    display: flex;
    align-items: center;
    padding: 20px;
    background: #f8f9fa;
    border-radius: 8px;
    transition: all 0.3s;
    position: relative;
}

.warning-message {
    margin: 20px;
}

.item-icon {
    font-size: 28px;
    margin-right: 20px;
    color: var(--el-color-primary);
    padding: 12px;
    background: rgba(99, 125, 255, 0.1);
    border-radius: 8px;
    flex-shrink: 0;
}

.item-content {
    flex: 1;
    margin-right: 20px;
}

.item-content h3 {
    margin: 0 0 8px;
    font-size: 16px;
    color: #333;
}

.item-content p {
    margin: 0;
    color: #666;
    font-size: 14px;
}

.account-info {
    color: rgba(62, 158, 241, 0.95) !important;
    font-size: 18px !important;
    ;
}

.action-btn {
    margin-left: auto;
    width: 100px;
    height: 35px;
    align-self: center;
    flex-shrink: 0;
}

@media (max-width: 768px) {
    .security-item {
        flex-wrap: wrap;
        padding: 15px;
    }

    .action-btn {
        margin-left: 0;
        width: 100%;
        margin-top: 15px;
    }

    .item-content {
        margin-right: 0;
        width: 100%;
    }
}

.code-input {
    display: flex;
    gap: 10px;
}
</style>