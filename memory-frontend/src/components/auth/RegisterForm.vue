<template>
    <el-card  :class="{'auth-card':true,'shareLogin':!showfoot}">
        <h2 class="auth-title">用户注册</h2>

        <el-form :model="formData" :rules="rules" ref="registerForm" @submit.prevent="handleSubmit">
            <!-- 账号 -->
            <el-form-item prop="account">
                <el-input v-model="formData.account" placeholder="请输入账号" prefix-icon="User" size="large" />
            </el-form-item>

            <!-- 邮箱 -->
            <el-form-item prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" prefix-icon="Message" size="large" />
            </el-form-item>

            <!-- 密码 -->
            <el-form-item prop="password">
                <el-input v-model="formData.password" type="password" placeholder="请输入密码" prefix-icon="Lock"
                    size="large" show-password />
            </el-form-item>

            <!-- 确认密码 -->
            <el-form-item prop="confirmPassword">
                <el-input v-model="formData.confirmPassword" type="password" placeholder="请确认密码" prefix-icon="Lock"
                    size="large" show-password />
            </el-form-item>

            <!-- 验证码 -->
            <el-form-item prop="code">
                <div class="code-input">
                    <el-input v-model="formData.code" placeholder="请输入6位验证码" prefix-icon="Key" size="large" />
                    <el-button type="primary" :disabled="codeCountdown > 0" @click="sendCode" class="code-button">
                        {{ codeButtonText }}
                    </el-button>
                </div>
            </el-form-item>

            <el-form-item>
                <el-button type="primary" size="large" native-type="submit" class="submit-btn" :loading="loading">
                    立即注册
                </el-button>
            </el-form-item>
        </el-form>

        <div class="auth-footer" v-if="showfoot">
            <span>已有账号？</span>
            <router-link to="/login">立即登录</router-link>
        </div>
    </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { accountRegister, getEmailCode } from '@/api/user/auth'
import $bus from '@/utils/bus.ts'
interface Props {
    showfoot: boolean,
}
const props = withDefaults(defineProps<Props>(), {
    showfoot: false,
})
const router = useRouter()
const registerForm = ref<FormInstance>()
const loading = ref(false)
// 验证码剩余时间
const codeCountdown = ref(0)
// 表单
const formData = reactive({
    account: '',
    email: '',
    password: '',
    confirmPassword: '',
    code: ''
})

const codeButtonText = computed(() => {
    return codeCountdown.value > 0
        ? `${codeCountdown.value}s后重发`
        : '获取验证码'
})

const validatePass2 = (rule: any, value: string, callback: any) => {
    if (value !== formData.password) {
        callback(new Error('两次输入密码不一致!'))
    } else {
        callback()
    }
}

const rules = reactive<FormRules>({
    account: [
        { required: true, message: '请输入账号', trigger: 'blur' },
        { min: 4, max: 16, message: '账号长度为4-16个字符', trigger: 'blur' },
        {
            pattern: /^[a-zA-Z0-9_]+$/,
            message: '账号只能包含字母、数字和下划线',
            trigger: 'blur'
        }
    ],
    email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 8, max: 20, message: '密码长度为8-20个字符', trigger: 'blur' },
        {
            pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
            message: '需包含大小写字母和数字',
            trigger: 'blur'
        }
    ],
    confirmPassword: [
        { required: true, message: '请再次输入密码', trigger: 'blur' },
        { validator: validatePass2, trigger: 'blur' }
    ],
    code: [
        { required: true, message: '请输入验证码', trigger: 'blur' },
        { len: 6, message: '验证码为6位数字', trigger: 'blur' }
    ]
})

const sendCode = async () => {
    try {
        await registerForm.value?.validateField('email')

        codeCountdown.value = 60
        localStorage.setItem('codeCountdown', codeCountdown.value.toString())
        const timer = setInterval(() => {
            codeCountdown.value--;
            localStorage.setItem('codeCountdown', codeCountdown.value.toString());
            if (codeCountdown.value <= 0) {
                clearInterval(timer)
                localStorage.removeItem('codeCountdown')
            }
        }, 1000)

        getEmailCode(formData).then((resp) => {
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
        })

    } catch (error) {
        console.error('验证码发送失败:', error)
    }
}

onMounted(() => {
    const storedCountdown = localStorage.getItem('codeCountdown')
    if (storedCountdown) {
        codeCountdown.value = parseInt(storedCountdown)
        const timer = setInterval(() => {
            codeCountdown.value--;
            localStorage.setItem('codeCountdown', codeCountdown.value.toString());
            if (codeCountdown.value <= 0) {
                clearInterval(timer)
                localStorage.removeItem('codeCountdown')
            }
        }, 1000)
    }
})

const handleSubmit = async () => {
    try {
        // 验证所有字段
        await registerForm.value?.validate()
        loading.value = true

        // 模拟API调用

        const resp = await accountRegister(formData);
        if (resp.code == 200) {
            ElMessage({
                type: 'success',
                message: resp.message
            });
            // 注册成功跳转到登录页
            if (props.showfoot)
                setTimeout(() => {
                    router.push('/login');
                }, 1000);
            else {
                $bus.emit('registerForm', false);
            }
        }
        else {
            ElMessage({
                type: 'error',
                message: resp.message
            });
        }


    } catch (error) {
        console.error('注册失败:', error)
        ElMessage.error('注册失败，请检查表单')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.auth-card {
    width: 100%;
    max-width: 440px;
    border-radius: 16px;
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.2);
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    z-index: 1;
    position: relative;
    overflow: hidden;
}
.shareLogin {
    box-shadow: 0 0 0 rgba(0, 0, 0, 0);
    background: transparent;
    backdrop-filter:0;
    border: 0;
}
@media (max-width: 768px) {

    /* 在屏幕宽度小于 768px 时应用 */
    .auth-card {
        max-width: 350px;
    }

    .brand-section .logo {
        font-size: 2.5rem;
    }
}

.auth-card::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(45deg,
            rgba(255, 255, 255, 0.1),
            rgba(255, 255, 255, 0.05));
    transform: rotate(15deg);
    z-index: -1;
}

.auth-card :deep(.el-card__body) {
    padding: 40px;
}

.auth-title {
    margin-top: 0px;
    text-align: center;
    color: #2d3748;
    margin-bottom: 30px;
    font-weight: 500;
    font-size: 1.8rem;
    position: relative;
}

.auth-title::after {
    content: '';
    position: absolute;
    bottom: -10px;
    left: 50%;
    transform: translateX(-50%);
    width: 60px;
    height: 3px;
    background: linear-gradient(90deg, #667eea, #764ba2);
    border-radius: 2px;
}

.login-type-switch {
    position: absolute;
    right: 20px;
    top: 20px;
}

.switch-button {
    padding: 8px 12px;
    color: #667eea;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s;
}

.switch-button:hover {
    color: #764ba2;
    background-color: rgba(102, 126, 234, 0.1);
    border-radius: 4px;
}

.code-input {
    display: flex;
    gap: 10px;
    align-items: center;
}

.code-input .el-input {
    flex: 1;
}

.code-button {
    width: 120px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    transition: all 0.3s;
}

.code-button:hover {
    opacity: 0.9;
    transform: translateY(-1px);
}

.submit-btn {
    width: 100%;
    margin-top: 10px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border: none;
    transition: all 0.3s;
}

.submit-btn:hover {
    opacity: 0.9;
    transform: translateY(-1px);
}

.auth-footer {
    text-align: center;
    color: #718096;
    margin-top: 20px;
}

.auth-footer a {
    color: #667eea;
    text-decoration: none;
    font-weight: 500;
    transition: color 0.3s;
}

.auth-footer a:hover {
    color: #764ba2;
    text-decoration: underline;
}

/* 表单输入框美化 */
:deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    transition: all 0.3s;
}

:deep(.el-input__wrapper:hover) {
    border-color: rgba(102, 126, 234, 0.5);
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

:deep(.el-input__wrapper.is-focus) {
    border-color: #667eea;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

/* 图标颜色 */
:deep(.el-input__prefix) {
    color: #667eea;
}
</style>