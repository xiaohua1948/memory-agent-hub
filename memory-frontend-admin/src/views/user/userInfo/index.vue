<template>
  <div class="app-container">
    <div style="margin:10px 10px 10px 0;">
      <el-input size="small" style="margin-right:10px" width="210px" class="set-input" v-model="searchValue"
        placeholder="请输入内容"></el-input>
      <el-button type="primary" size="small">新增用户</el-button>
      <el-button size="small" type="primary" icon="el-icon-search">搜索</el-button>
      <el-button size="small" :loading="downloadLoading" type="primary" icon="el-icon-download" @click="handleDownload">
        导出
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="userInfos" element-loading-text="Loading" border fit>
      <!-- 序号 -->
      <el-table-column align="center" label="序号" width="60">
        <template slot-scope="scope">{{ scope.$index + 1 }}</template>
      </el-table-column>

      <!-- 头像 -->
      <el-table-column prop="avatarUrl" label="头像" width="90" align="center">
        <template slot-scope="scope">
          <el-avatar :size="55" :src="scope.row.avatarUrl"></el-avatar>
        </template>
      </el-table-column>

      <!-- 用户ID -->
      <el-table-column prop="userId" label="用户ID" width="90" align="center"></el-table-column>

      <!-- 账号 -->
      <el-table-column prop="account" label="账号" width="110" align="center"></el-table-column>

      <!-- 名称 -->
      <el-table-column prop="userName" label="名称" width="100" align="center"></el-table-column>

      <!-- 身份 -->
      <el-table-column prop="role" label="身份" width="80" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.role | statusFilter">{{ scope.row.role }}</el-tag>
        </template>
      </el-table-column>

      <!-- 存储空间 -->
      <el-table-column label="总空间" width="100" align="center">
        <template slot-scope="scope">
          {{ scope.row.totalSpace | formatStorage }}
        </template>
      </el-table-column>

      <el-table-column label="已用空间" width="100" align="center">
        <template slot-scope="scope">
          {{ scope.row.usedSpace | formatStorage }}
        </template>
      </el-table-column>

      <!-- 账户状态 -->
      <el-table-column label="账户状态" width="110" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.accountStatus | statusTagFilter">
            {{ scope.row.accountStatus | statusTextFilter }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 会员天数 -->
      <el-table-column label="剩余天数" width="100" align="center">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.membershipDays > 0 ? '#67C23A' : '#909399' }">
            {{ scope.row.membershipDays }} 天
          </span>
        </template>
      </el-table-column>

      <!-- 简介 -->
      <el-table-column prop="profile" label="简介" align="center"></el-table-column>

      <!-- 邮箱 -->
      <el-table-column prop="email" label="邮箱" width="120" align="center"></el-table-column>

      <!-- 注册时间 -->
      <el-table-column label="注册时间" align="center">
        <template slot-scope="scope">
          <i class="el-icon-time" />
          <span>{{ formattedCreateTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>

      <!-- 更新时间 -->
      <el-table-column label="更新时间" align="center">
        <template slot-scope="scope">
          <i class="el-icon-time" />
          <span>{{ formattedCreateTime(scope.row.updateTime) }}</span>
        </template>
      </el-table-column>

      <!-- 编辑操作 -->
      <!-- 原表格中的编辑列修改 -->
      <el-table-column label="编辑" align="center" width="120">
        <template slot-scope="scope">
          <el-button type="primary" size="small" @click="handleEdit(scope.row)">修改</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <div style="margin-top:20px;display:flex;justify-content:center">
      <el-pagination background @size-change="handleSizeChange" @current-change="handleCurrentChange"
        :current-page="currentPage" :page-sizes="pageSizes" :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper" :total="total">
      </el-pagination>
    </div>

    <el-dialog title="用户信息修改" :visible.sync="dialogVisible" width="600px" @closed="handleDialogClose">

      <el-form ref="editForm" :model="editForm" label-width="80px" :rules="formRules">

        <!-- 用户ID（不可编辑） -->
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model="editForm.userId" disabled></el-input>
        </el-form-item>

        <!-- 账号 -->
        <el-form-item label="账号" prop="account">
          <el-input v-model="editForm.account" disabled></el-input>
        </el-form-item>

        <!-- 用户名 -->
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="editForm.userName"></el-input>
        </el-form-item>

        <!-- 身份 -->
        <el-form-item label="身份" prop="role">
          <el-select v-model="editForm.role" disabled>
            <el-option label="普通用户" value="user"></el-option>
            <el-option label="管理员" value="admin"></el-option>
          </el-select>
        </el-form-item>

        <!-- 账户状态 -->
        <el-form-item label="账户状态" prop="accountStatus">
          <el-select v-model="editForm.accountStatus">
            <el-option label="普通用户" value="normal"></el-option>
            <el-option label="VIP会员" value="vip"></el-option>
            <el-option label="超级会员" value="svip"></el-option>
          </el-select>
        </el-form-item>

        <!-- 存储空间 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总空间(GB)" prop="totalSpace">
              <el-input-number v-model="editForm.totalSpace" :min="1" :step="1"
                controls-position="right"></el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已用空间(GB)" prop="usedSpace">
              <el-input-number disabled v-model="editForm.usedSpace" :min="0" :max="editForm.totalSpace"
                controls-position="right"></el-input-number>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 会员天数 -->
        <el-form-item label="会员天数" prop="membershipDays">
          <el-input-number v-model="editForm.membershipDays" :min="0" controls-position="right"></el-input-number>
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" disabled></el-input>
        </el-form-item>

        <!-- 简介 -->
        <el-form-item label="简介" prop="profile">
          <el-input type="textarea" :rows="3" v-model="editForm.profile"></el-input>
        </el-form-item>
      </el-form>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitEdit">确 定</el-button>
      </span>
    </el-dialog>
  </div>



</template>

<script>

import { selectAllUserInfo, updateUserInfo } from '@/api/user'

export default {
  filters: {
    statusFilter(role) {
      const statusMap = {
        user: 'info',
        VIP1: 'gray',
        VIP2: 'warning',
        VIP3: 'success',
        SVIP: 'danger'
      }
      return statusMap[role] || 'info'
    },
    // 新增过滤器
    formatStorage(bytes) {
      if (bytes === 0) return '0 B';
      const GB = bytes / (1024 * 1024 * 1024);
      return `${GB.toFixed(2)} GB`;
    },
    statusTagFilter(status) {
      const map = { normal: 'info', vip: 'warning', svip: 'danger' };
      return map[status] || 'info';
    },
    statusTextFilter(status) {
      const map = { normal: '普通用户', vip: 'VIP会员', svip: '超级会员' };
      return map[status] || '未知状态';
    }
  },
  data() {
    return {
      userInfos: [],
      listLoading: true,
      total: 0,
      currentPage: 1,
      pageSize: 20,
      pageSizes: [20, 30, 40, 50],
      searchValue: '',
      downloadLoading: false,
      // 新增对话框相关
      dialogVisible: false,
      editForm: {
        userId: '',
        account: '',
        userName: '',
        role: 'user',
        totalSpace: 0,
        usedSpace: 0,
        accountStatus: 'normal',
        membershipDays: 0,
        email: '',
        profile: ''
      },
      formRules: {
        account: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { min: 4, max: 20, message: '长度在4到20个字符', trigger: 'blur' }
        ],
        userName: [
          { required: true, message: '请输入用户名', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
        ]
      }
    }
  },
  created() {
    this.loadAllUser()
  },
  methods: {
    formattedCreateTime(time) {
      const date = new Date(time);
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`;
    },
    loadAllUser() {
      this.listLoading = true
      selectAllUserInfo({
        current: this.currentPage,
        pageSize: this.pageSize,
        keyword: this.searchValue
      }).then(res => {
        if (res.code === 200) {
          this.userInfos = res.data || []
          this.total = res.data.total || 0
        }
      }).finally(() => {
        this.listLoading = false
      })
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/utils/vendor/Export2Excel').then(excel => {
        const filterVal = [
          'avatarUrl', 'userId', 'account', 'userName', 'role',
          'totalSpace', 'usedSpace', 'accountStatus', 'membershipDays',
          'profile', 'email', 'createTime', 'updateTime'
        ]
        const tHeader = [
          '头像', '用户ID', '账号', '名称', '身份',
          '总空间', '已用空间', '账户状态', '剩余天数',
          '简介', '邮箱', '注册时间', '更新时间'
        ]
        const data = this.formatJson(filterVal)
        excel.export_json_to_excel({
          header: tHeader,
          data,
          filename: '用户信息表',
          autoWidth: true
        })
        this.downloadLoading = false
      })
    },
    handleEdit(row) {
      this.dialogVisible = true
      // 深拷贝用户数据
      this.editForm = {
        ...row,
        // 转换存储单位（GB转字节）
        totalSpace: (row.totalSpace / (1024 * 1024 * 1024)),
        usedSpace: (row.usedSpace / (1024 * 1024 * 1024)).toFixed(2)
      }
    },

    // 提交修改
    submitEdit() {
      this.$refs.editForm.validate(valid => {
        if (valid) {
          const params = {
            ...this.editForm,
            // 转换存储单位为字节
            totalSpace: this.editForm.totalSpace * (1024 ** 3),
            usedSpace: this.editForm.usedSpace * (1024 ** 3)
          }

          updateUserInfo(params).then(res => {
            if (res.code === 200) {
              this.$message.success('修改成功')
              this.dialogVisible = false
              // 更新表格数据
              const index = this.userInfos.findIndex(item => item.userId === params.userId)
              this.userInfos.splice(index, 1, params)
            }
          })
        }
      })
    },

    // 对话框关闭时重置表单
    handleDialogClose() {
      this.$refs.editForm.resetFields()
    },
    formatJson(filterVal) {
      return this.userInfos.map(v => filterVal.map(j => {
        if (j.includes('Time')) {
          return this.formattedCreateTime(v[j])
        }
        return v[j]
      }))
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.loadAllUser()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.loadAllUser()
    }
  }
}
</script>

<style scoped>
.set-input {
  width: 210px !important;
}
</style>