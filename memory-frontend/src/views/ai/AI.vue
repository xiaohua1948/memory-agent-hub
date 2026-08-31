<template>
    <div id="picture" class="content">
        <!-- 顶部菜单 -->
        <div class="content-header">
            <div class="content-header-left">
                <h3 style="margin-left: 20px;   white-space: nowrap;">图像分类</h3>
                <el-button @click="dialogPicutureVisible = true" type="success" round
                    class="content-header-left-button">
                    <span class="content-header-button-span">{{ tableData.length > 0 ? "重新选择" : "选择照片" }}</span>
                </el-button>



                <span style="margin-left: 20px;   white-space: nowrap;">标签数量：</span>
                <el-input-number style="width: 120px;" v-model="tagCount" @change="handleTagNumChange" :precision="0"
                    :step="1" />
                <el-tooltip content="开启后,系统将自动设置图片标签(confidence最高的识别结果)" :hide-after="1000" placement="right"
                    effect="customized">
                    <el-switch style="margin-left: 20px;" v-model="IsAutoAddTag" active-text="自动" inactive-text="手动" />
                </el-tooltip>


            </div>
            <div class="content-header-right">
                <el-button @click="startGetPictureTag" type="primary" round class="content-header-left-button">
                    <span class="content-header-button-span">{{ IsAI ? "重新识别" : "开始识别" }}</span>
                </el-button>
                <el-button @click="setAllPictureTag" type="primary" round>
                    设置全部
                </el-button>
            </div>
        </div>

        <el-divider />

        <div id="picture-table">
            <el-table :header-cell-style="{ 'text-align': 'center' }"
                :data="tableData.slice((currentPage - 1) * pageSize, currentPage * pageSize)" class="picture-table-data"
                :row-class-name="tableRowClassName" empty-text="暂无数据，请选择图片">
                <el-table-column prop="thumbnailUrl" label="图片" width="90">
                    <template #default="{ row, $index }">
                        <el-tooltip placement="bottom">
                            <template #content> 预览图片</template>
                            <el-image :src="row.thumbnailUrl" class="thumbnail" style="cursor: pointer;"
                                @click="() => { isPreviewImage = true; currentImageIndex = (currentPage - 1) * pageSize + $index }">
                            </el-image>
                        </el-tooltip>

                    </template>
                </el-table-column>
                <el-table-column prop="contentType" label="照片类型" width="110" />
                <el-table-column prop="size" label="照片大小" width="90" :formatter="(row) => formatFileSize(row.size)" />

                <el-table-column prop="originFileName" label="照片名称" width="120" />
                <el-table-column prop="tags" label="AI识别结果(照片类别/标签/置信度)">
                    <template #default="{ row, $index }">
                        <div v-loading="loadingStates[(currentPage - 1) * pageSize + $index]"
                            element-loading-background="rgba(255, 255, 255, 0)" element-loading-text="识别中..."
                            style="height: 80px;overflow-y: scroll;overflow: auto;">
                            <el-radio-group v-model="row.selectedTagIndex" @change="handleTagChange(row)"
                                v-if="(!loadingStates[(currentPage - 1) * pageSize + $index])" class="tag-container">
                                <div v-for="(num, index) in tagCount" :key="index" v-if="row.tags.length > 0">
                                    <el-radio v-if="index < row.tags.length" :value="index">
                                        <el-text type="primary" style="margin-left: 5px;"> {{ row.tags[index]?.imageType
                                            }} -</el-text>
                                        <el-text type="success" style="margin-left: 5px;"> {{ row.tags[index]?.tagName
                                            }} -</el-text>
                                        <el-text type="warning" style="margin-left: 5px;"> {{
                                            row.tags[index]?.confidence }}%</el-text>

                                    </el-radio>
                                </div>
                            </el-radio-group>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="originFileName" label="识别照片" width="80">
                    <template #default="{ row, $index }">
                        <el-button type="primary" round color="#626aef"
                            @click="getOnePictureTag(row, (currentPage - 1) * pageSize + $index)">
                            <el-icon>
                                <ZoomIn />
                            </el-icon>
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column prop="originFileName" label="设置标签" width="80">
                    <template #default="{ row }">
                        <el-button type="primary" round color="#626aef" @click="setOnePictureTag(row)"
                            :disabled="row.tags.length == 0">
                            <el-icon>
                                <Setting />
                            </el-icon>
                        </el-button>
                    </template>
                </el-table-column>
                <el-table-column width="20">

                </el-table-column>
            </el-table>
            <div class="pagination-container">
                <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize"
                    :page-sizes="[5, 8, 10, 15, 20, 30, 50]" :total="tableData.length"
                    layout="total, sizes, prev, pager, next, jumper" background @size-change="handleSizeChange"
                    @current-change="handleCurrentChange">

                </el-pagination>
            </div>
        </div>

        <el-image-viewer :url-list="previewSrcList" :teleported="true" :initial-index="currentImageIndex"
            v-if="isPreviewImage" @close="() => { isPreviewImage = false }">
        </el-image-viewer>

        <el-dialog class="select-picture-diag" width="90%" v-model="dialogPicutureVisible" title="选择照片"
            :append-to-body="true">
            <el-tabs v-model="activeName" @tab-click="handleTabClick" style="height: 100%;width: 100%;">
                <el-tab-pane label="照片" name="picture" style="height:100% ;width: 100%;">
                    <SelectPicture />
                </el-tab-pane>
                <el-tab-pane label="相册" name="album"></el-tab-pane>
            </el-tabs>
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import $bus from '@/utils/bus.ts'
import SelectPicture from '@/components/select_picture/SelectPicture.vue';
import { getPictureTag, addPictureTag, addSomePictureTag } from '@/api/file/file'
onMounted(() => {
    showBus();
});
const tableData = ref<API.SelectPicture[]>([

]);
// 新增图片预览相关变量
const isPreviewImage = ref(false);
const currentImageIndex = ref(0);

const IsAI = ref(false);
// 在原有代码基础上新增分页相关变量
const currentPage = ref(1);
const pageSize = ref(5);
const total = ref(0);
const previewSrcList = ref<string[]>([]);
// 新增分页处理方法
const handleSizeChange = (newSize: number) => {
    pageSize.value = newSize;
    currentPage.value = 1; // 切换每页数量时重置到第一页
};

const handleCurrentChange = (newPage: number) => {
    currentPage.value = newPage;
};
const showBus = () => {
    //获取更多数据
    $bus.off('addSelectPicture');
    $bus.on('addSelectPicture', (val) => {
        const selectPictureList = <API.SelectPicture[]>val;
        tableData.value = selectPictureList;
        previewSrcList.value = tableData.value.map(item => item.thumbnailUrl);
        dialogPicutureVisible.value = false;
    })
}
const IsAutoAddTag = ref(false);
const tagCount = ref(10);
const loadingStates = ref<boolean[]>([]);
// 可用标签列表

const tableRowClassName = ({ rowIndex }: { rowIndex: number }) => {
    return rowIndex % 2 === 0 ? 'even-row' : 'odd-row';
};
const handleTagNumChange = () => {
    tableData.value.forEach((item) => {
        if (item.selectedTagIndex >= tagCount.value)
            item.selectedTagIndex = 0;
    })
};
const dialogPicutureVisible = ref(false);
const activeName = ref('picture');

const handleTabClick = (tab: any) => {
};

const handleTagChange = (row: API.SelectPicture) => {
};
const setAllPictureTag = () => {

    if (tableData.value.length === 0) {
        ElMessage.warning('没有选择图片');
        return;
    }
    const pictureTags: API.PictureTag[] = [];
    tableData.value.forEach((item, index) => {
        if (item.tags.length > 0) {
            const selectedTag = item.tags[item.selectedTagIndex];
            pictureTags.push({
                id: index,
                fileId: item.fileId,
                tagName: selectedTag.tagName,
                imageType: selectedTag.imageType,
            });
        }
    })
    ElMessageBox.confirm(
        '确认设置所有图片的标签吗？',
        '设置标签',
        {
            confirmButtonText: "确认",
            cancelButtonText: '取消',
            type: 'info',
        }
    )
        .then(async () => {
            const res = await addSomePictureTag(
                pictureTags
            )
            if (res.code === 200) {
                ElMessage({
                    type: 'success',
                    message: '设置成功',
                })
            }
        })
}
const setOnePictureTag = (row: API.SelectPicture) => {
    if (row.tags.length === 0) {
        ElMessage.warning('没有识别标签');
        return;
    }
    let item = row.tags[row.selectedTagIndex];
    ElMessageBox.confirm(
        item.imageType + "-" + item.tagName + "-" + item.confidence + "%",
        '设置标签',
        {
            confirmButtonText: "确认",
            cancelButtonText: '取消',
            type: 'info',
        }
    )
        .then(async () => {
            const res = await addPictureTag({
                fileIds: [row.fileId],
                tagName: item.tagName,
                imageType: item.imageType,
            })
            if (res.code === 200) {
                ElMessage({
                    type: 'success',
                    message: '设置成功',
                })
            }

        })
        .catch(() => {
            ElMessage({
                type: 'info',
                message: '取消设置',
            })
        })
}
const getOnePictureTag = async (row: API.SelectPicture, index: number) => {
    if (tableData.value.length === 0) {
        ElMessage.warning('请先选择图片')
        return;
    }

    loadingStates.value[index] = true;


    const item = tableData.value[index];
    try {

        const result = await getPictureTag({
            autoAddTag: IsAutoAddTag.value,
            fileId: item.fileId,
            thumbnailObjectName: item.thumbnailObjectName,
        });
        item.tags = result.data;
        item.selectedTagIndex = 0;

    } catch (error) {
        ElMessage.error("图片 " + item.originFileName + "识别失败");
    } finally {
        loadingStates.value[index] = false;
    }

}

const startGetPictureTag = async () => {

    if (tableData.value.length === 0) {
        ElMessage.warning('请先选择图片')
        return;
    }

    for (let i = 0; i < tableData.value.length; i++) {
        loadingStates.value[i] = true;
    }
    IsAI.value = true;
    for (let i = 0; i < tableData.value.length; i++) {
        // 通过索引设置loading
        const item = tableData.value[i];
        try {

            const result = await getPictureTag({
                autoAddTag: IsAutoAddTag.value,
                fileId: item.fileId,
                thumbnailObjectName: item.thumbnailObjectName,
            });
            item.tags = result.data;
            item.selectedTagIndex = 0;

        } catch (error) {
            ElMessage.error("图片 " + item.originFileName + "识别失败");
        } finally {
            loadingStates.value[i] = false;
        }
    }
}
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

</script>

<style scoped>
#picture-table {
    width: 100%;
    height: calc(100% - 80px);
    overflow-y: auto;
    position: relative;
}

.picture-table-data {
    width: 100%;
    height: calc(100% - 60px);
    overflow-y: auto;
}

.pagination-container {

    position: absolute;
    bottom: 10px;
    left: 50px;
}

/* 图片大小 */
.thumbnail {
    width: 60px;
    height: 60px;
    object-fit: cover;
    border-radius: 4px;
}

.tag-container {
    display: flex;
    flex-wrap: wrap;
    column-gap: 20px;
}

.content {
    width: 100%;
    height: 100%;
    position: relative;
}

.content-header {
    z-index: 4;
    width: 100%;
    height: 60px;
    display: flex;
}

.content-header-left-button {
    width: 100px;
    margin-left: 20px;
}

.content-header-left {
    height: 100%;
    display: flex;
    align-items: center;
    margin-right: auto;

}

.content-header-right {
    height: 100%;
    width: 200px;
    margin-right: 10px;
    display: flex;
    align-items: center;
    justify-content: center;

}

.el-divider--horizontal {
    margin-top: 0px;
    margin-bottom: 0px;
}

:deep(.el-table .even-row) {
    background-color: #fafafa;
}

:deep(.el-table .odd-row) {
    background-color: #ffffff;
}




.select-picture-diag {
    height: 500px;
}

.select-picture-tabs {
    width: 100%;
    height: 500px;
}

:deep(.el-tabs__header) {
    margin: 0;
    height: 40px;
    padding: 0;
}

/* 自定义加载css */
:deep(.el-loading-spinner) {
    position: relative;
    top: 30px;
}

:deep(.el-loading-parent--relative) {
    height: 72px;
}

:deep(.el-loading-spinner) {
    .circular {
        width: 30px;
        height: 30px;

        .path {
            stroke-width: 3;
        }
    }

    .el-loading-text {
        font-size: 14px;
        margin-top: 6px;
    }
}
</style>
<style>
.select-picture-diag {
    height: calc(100% - 100px) !important;
    margin: 0 auto !important;
    margin-top: 40px !important;
}

.el-dialog__body {
    height: calc(100% - 50px);
    width: 100%;
}

.el-popper.is-customized {
    /* Set padding to ensure the height is 32px */
    padding: 6px 12px;
    background: linear-gradient(90deg, rgb(159, 229, 151), rgb(204, 229, 129));
}

.el-popper.is-customized .el-popper__arrow::before {
    background: linear-gradient(45deg, #b2e68d, #bce689);
    right: 0;
}
</style>