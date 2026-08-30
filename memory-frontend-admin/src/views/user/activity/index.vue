<template>
  <div class="app-container">
    <div style="margin:10px 10px 10px 0;">
      <el-input size="small" style="margin-right:10px" width="210px" 
                class="set-input" v-model="searchValue"
                placeholder="请输入内容"></el-input>
      <el-button size="small" type="primary" icon="el-icon-search">搜索</el-button>
      <el-button type="primary" size="small">新增用户</el-button>
      <el-button size="small" :loading="downloadLoading" 
                type="primary" icon="el-icon-download" @click="handleDownload">
        导出
      </el-button>
    </div>

    <el-table v-loading="listLoading" :data="activities" 
             element-loading-text="Loading" border fit>
      <el-table-column align="center" label="序号" width="60">
        <template slot-scope="scope">{{ scope.$index + 1 }}</template>
      </el-table-column>

      <el-table-column prop="id" label="ID" width="90" align="center"/>
      <el-table-column prop="userId" label="用户ID" width="140" align="center"/>
      <el-table-column prop="operation" label="行为" width="200" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.operation | statusFilter">
            {{ scope.row.operation }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="ipv4" label="IP地址" align="center"/>

      <el-table-column label="操作时间" width="160" align="center">
        <template slot-scope="scope">
          <i class="el-icon-time"/>
          <span>{{ formattedCreateTime(scope.row.operationTime) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button type="danger" size="small">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top:20px;display:flex;justify-content:center">
      <el-pagination background 
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    :current-page="currentPage" 
                    :page-sizes="pageSizes"
                    :page-size="pageSize"
                    layout="total, sizes, prev, pager, next, jumper" 
                    :total="total"/>
    </div>
  </div>
</template>

<script>

import { selectAllRecord } from '@/api/user'

export default {
  filters: {
    statusFilter(operation) {
      const statusMap = {
        '创建相册': 'success',
        '删除照片': 'danger',
        '上传文件': 'warning'
      }
      return statusMap[operation.split(':')[0]] || ''
    }
  },
  data() {
    return {
      activities: [],
      listLoading: true,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      pageSizes: [5, 10, 15, 20,30,50],
      searchValue: '',
      downloadLoading: false
    }
  },
  created() {
    this.selectAllRecord();
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
    selectAllRecord() {
      this.listLoading = true
      selectAllRecord({
        current: this.currentPage,
        pageSize: this.pageSize
      }).then(res => {
        if (res.code === 200) {
          this.activities = res.data.records.map(item => ({
            id: item.id,
            userId: item.userId,
            operation: item.operation,
            ipv4: item.ipv4,
            operationTime: item.operationTime
          }))
          this.total = res.data.total
        }
      }).finally(() => {
        this.listLoading = false
      })
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/utils/vendor/Export2Excel').then(excel => {
        const filterVal = ['id', 'operation', 'ipv4', 'operationTime']
        const tHeader = ['ID', '用户行为', 'IP地址', '操作时间']
        const data = this.activities.map(item => [
          item.id,
          item.operation,
          item.ipv4,
          this.formattedCreateTime(item.operationTime)
        ])
        excel.export_json_to_excel({
          header: tHeader,
          data,
          filename: '用户行为记录表'
        })
        this.downloadLoading = false
      })
    },
    handleSizeChange(val) {
      this.pageSize = val
      this.selectAllRecord()
    },
    handleCurrentChange(val) {
      this.currentPage = val
      this.selectAllRecord()
    }
  }
}
</script>

<style scoped>
.set-input {
  width: 210px !important;
}
</style>