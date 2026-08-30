<template>
    <div class="app-container">
    <!-- 新增按钮 -->
    <!-- 新增按钮 -->
    <el-button 
      type="primary" 
      size="small" 
      style="margin-bottom:20px;"
      @click="showAddDialog">
      + 新增角色
    </el-button>
  

    <el-table v-loading="listLoading" :data="adminInfo" element-loading-text="Loading" border fit highlight-current-row>
      <el-table-column align="center" label="序号" width="95">
        <template slot-scope="scope">
          {{ scope.$index + 1 }}
        </template>
      </el-table-column>

      <el-table-column label="管理员ID" width="120">
        <template slot-scope="scope">
          {{ scope.row.userId }}
        </template>
      </el-table-column>

      <el-table-column label="管理员账号" width="110" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.account }}</span>
        </template>
      </el-table-column>

      <el-table-column label="管理员名称" width="110" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.userName }}</span>
        </template>
      </el-table-column>

      <el-table-column class-name="status-col" label="管理员权限" width="110" align="center">
        <template slot-scope="scope">
          <el-tag type="danger">{{ scope.row.role }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column class-name="status-col" label="编辑权限" width="200" align="center">
        <template slot-scope="scope">
          <el-button type="primary" size="small">修改</el-button>
          <el-button type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>

      <el-table-column align="center" prop="created_at" label="创建时间" width="200">
        <template slot-scope="scope">
          <i class="el-icon-time" />
          <span>{{formattedCreateTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>

      <el-table-column align="center" prop="created_at" label="更新时间" width="200">
        <template slot-scope="scope">
          <i class="el-icon-time" />
          <span>{{ formattedCreateTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>
    </el-table>



      <!-- 新增管理员对话框 -->
      <el-dialog
      title="新增管理员"
      :visible.sync="addDialogVisible"
      width="40%"
      @closed="resetForm">
      <el-form 
        :model="adminForm" 
        :rules="formRules" 
        ref="adminFormRef"
        label-width="120px">
        <!-- 账号 -->
        <el-form-item label="管理员账号" prop="account">
          <el-input 
            v-model="adminForm.account" 
            placeholder="4-16位字母数字组合"
            clearable
            maxlength="16"/>
        </el-form-item>

        <!-- 密码 -->
        <el-form-item label="登录密码" prop="password">
          <el-input
            type="password"
            v-model="adminForm.password"
            placeholder="6-20位字符"
            show-password
            maxlength="20"/>
        </el-form-item>

        <!-- 邮箱验证模块 -->
        <el-form-item label="邮箱" prop="email">
          <el-input 
            v-model="adminForm.email"
            placeholder="请输入有效邮箱"
            style="width: 100%">
            <template slot="append">
              <el-button 
                :disabled="isSending || countdown > 0"
                @click="sendVerifyCode">
                {{ countdown > 0 ? `${countdown}秒后重发` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <!-- 验证码 -->
        <el-form-item label="验证码" prop="vcode">
          <el-input 
            v-model="adminForm.vcode"
            placeholder="6位数字验证码"
            maxlength="6"
            clearable/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button 
          type="primary"
          :loading="submitLoading"
          @click="submitAdminForm">
          确定
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { selectAllAdmin,addAdmin,sendEmailCode } from '@/api/user'

export default {
  data() {
    return {
      adminInfo: [],
      listLoading: false,
      addDialogVisible: false,
      submitLoading: false,
      isSending: false,
      countdown: 0,
      
      adminForm: {
        account: '',
        password: '',
        email: '',
        vcode: ''
      },

      formRules: {
        account: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { pattern: /^[a-zA-Z0-9]{4,16}$/, message: '4-16位字母数字组合' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
        ],
        email: [
          { required: true, message: '请输入邮箱地址', trigger: 'blur' },
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ],
        vcode: [
          { required: true, message: '请输入验证码', trigger: 'blur' },
          { pattern: /^\d{6}$/, message: '验证码为6位数字' }
        ]
      }
    }
  },
  methods: {  // 显示对话框（网页8）
    // 显示对话框
    showAddDialog() {
      this.addDialogVisible = true
      this.$nextTick(() => this.$refs.adminFormRef.clearValidate())
    },

    // 发送验证码（参考网页6、网页7）
    async sendVerifyCode() {
      try {
        await this.$refs.adminFormRef.validateField('email')
        this.isSending = true
        
        const res = await sendEmailCode({ 
          email: this.adminForm.email 
        })
        
        if(res.code === 200) {
          this.$message.success(res.message)
          this.startCountdown()
        }
      } catch(error) {
        console.error('发送失败:', error)
      } finally {
        this.isSending = false
      }
    },

    // 倒计时处理
    startCountdown() {
      this.countdown = 60
      const timer = setInterval(() => {
        if(this.countdown <= 0) {
          clearInterval(timer)
          return
        }
        this.countdown--
      }, 1000)
    },

    // 提交表单（参考网页3）
    submitAdminForm() {
      this.$refs.adminFormRef.validate(async valid => {
        if(!valid) return
        
        this.submitLoading = true
        try {
          const res = await addAdmin(this.adminForm)
          if(res.code === 200) {
            this.$message.success('添加成功')
            this.addDialogVisible = false
            this.loadAdminList()
          }
        } finally {
          this.submitLoading = false
        }
      })
    },

    // 重置表单
    resetForm() {
      this.$refs.adminFormRef.resetFields()
      this.countdown = 0
    },

    // 加载管理员列表
    async loadAdminList() {
      this.listLoading = true
      try {
        const res = await selectAllAdmin()
        if(res.code === 200) this.adminInfo = res.data
      } finally {
        this.listLoading = false
      }
    },
 
    formattedCreateTime(time) {
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`;
    } 
  },
  created() {
    this.listLoading = true;
    selectAllAdmin().then(res => {
      if (res.code == 200) {
       
        this.adminInfo = res.data;
      }
    }).catch(error => {
      console.error("加载数据失败:", error);
    }).finally(() => {
      this.listLoading = false;
    });
  }
}
</script>