<template>
    <div class="record-container">
        <div class="content-header">
            操作记录
        </div>
        <el-divider />
        <div class="record-table">
            <!-- 数据表格 -->
            <el-table :data="records" style="width: calc(100% - 40px)  ;margin: 20px;">
                <el-table-column prop="operationTime" label="操作时间">
                    <template #default="scope">
                        {{ formatDate(scope.row.operationTime) }}
                    </template>
                </el-table-column>
                <el-table-column prop="operation" label="操作" width="450" />
                <el-table-column prop="number" label="数量" />
                <el-table-column prop="ipv4" label="IPv4 地址" width="300"/>
                <el-table-column prop="userId" label="用户ID" />
            </el-table>
        </div>
        <!-- 分页组件 -->
        <el-pagination class="pagination" @size-change="handleSizeChange" @current-change="handleCurrentChange"
            total-text="照片总数" prev-text="上一页" next-text="下一页" :current-page="currentPage"
            :page-sizes="[5, 10,20, 30,50]" :page-size="pageSize" layout="total, sizes, prev, pager, next, jumper"
            :total="total">
        </el-pagination>
    </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue';
import { selectAll } from '@/api/record/record';
// 定义记录接口
interface Record {
    operationTime: string; // 根据实际后端返回类型调整
    operation: string;
    number: number;
    ipv4: string;
    userId: number;
}
onMounted(() => {
    loadRecord();

});
const loadRecord =  async () => {
    const res = await selectAll({
        current: currentPage.value,
        size: pageSize.value,
    });
    if (res.code != 200) {
        return
    }
    total.value = res.data.total;

    records.value = res.data.records;

}
// 示例数据，实际项目中应通过 API 获取
const records = ref<Record[]>([
    {
        operationTime: '2023-10-01T10:15:30Z',
        operation: '上传图片',
        number: 5,
        ipv4: '192.168.1.1',
        userId: 1001,
    },
    // ...更多记录
]);
const total = ref(0)
const currentPage = ref(1); // 当前页码
const pageSize = ref(10); // 每页显示的记录数

// 计算分页后的记录

// 格式化日期的方法
const formatDate = (dateStr: string): string => {
    const date = new Date(dateStr);
    return date.toLocaleString(); // 可根据需要调整格式
};

// 处理每页大小变化
const handleSizeChange = (newSize: number) => {
    
    pageSize.value = newSize;
    currentPage.value = 1; // 重置到第一页
    loadRecord();
};

// 处理当前页码变化
const handleCurrentChange = (newPage: number) => {
  
    currentPage.value = newPage;
    loadRecord();
};
</script>

<style scoped>
.record-container {
    width: calc(100% - 40px);
    height: calc(100% - 90px);

}

.record-table {
    width: 100%;
    height: calc(100% - 80px);
  
    overflow: auto;
    position: relative;
}

.pagination {
    margin-left: 30px;
    margin-top: 30px;
    text-align: right;
}

.content-header {
    font-size: 20px;
    color: #333;
    letter-spacing: 0;
    padding: 20px;
}

/* 自定义ElementUI分割线 */
.el-divider--horizontal {
    margin-top: 0px;
    margin-bottom: 0px;
}
</style>