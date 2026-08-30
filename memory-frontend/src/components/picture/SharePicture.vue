<template>
    <el-dialog draggable :append-to-body="true" v-model="dialogVisible" :close-on-click-modal="false" :modal="true"
        :title="type=='picture'?'分享照片':'分享相册'" width="400" center @closed="close">
        <div class="share-image">
            <div class="image-container" :class="`count-${Math.min(shareImageUrlList.length, 3)}`">
                <el-image v-for="(item, index) in shareImageUrlList" :key="index" :src="item" alt="" fit="cover" />
                <div class="image-count">共{{ type == 'picture' ? shareFileIds.length : shareAlbumIds.length }}张{{ type ==
                    'picture' ? '照片' : '相册' }}</div>
            </div>
        </div>
        <div class="share-time">
            <span class="share-time-title">有效期:</span>
            <el-radio-group v-model="shareDay" style="margin: 0;padding: 0;">
                <el-radio :value="1">1天</el-radio>
                <el-radio :value="7">7天</el-radio>
                <el-radio :value="30">30天</el-radio>
                <el-radio :value="-1">永久</el-radio>
            </el-radio-group>
        </div>

        <el-button v-if="isShareSuccess" link type="primary" class="share-button" size="large"
            @click="copyShareUrl">复制链接</el-button>

        <el-button v-else color="#3174ff" type="primary" class="share-button" @click="createShare">创建分享</el-button>
    </el-dialog>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue'
import { createShareUrl } from '@/api/file/file'
import { createShareAlbumUrl } from '@/api/album/album'
import $bus from '@/utils/bus.ts'
interface Props {
    type: string,
    show: boolean
    checkedImages: string[][],
    fileInfoList: API.FileInfoList[],
    checkedAlbums: number[],
    albumInfoList: API.albumInfo[]
}
const props = defineProps<Props>()
// 分享期限
const shareDay = ref(1);
//分享链接是否创建成功
const isShareSuccess = ref(false);
//回传父组件
const emit = defineEmits<{ (e: "update:show", value: boolean): void }>()
//分享照片的src url
const shareImageUrlList = ref(['']);
//分享照片的fileId
const shareFileIds = ref(props.checkedImages.flat());
const shareAlbumIds = ref(props.checkedAlbums);
// 展示分享的对话框
const dialogVisible = ref(false);
//分享链接
const shareUrl = ref(`${window.location.origin}/share/`);
//创建分享链接
const createShare = async () => {
    let res = null;
    if (props.type == 'album') {
        res = await createShareAlbumUrl({
            albumIds: shareAlbumIds.value,
            shareDay: shareDay.value,
        })
    }
    else {
        res = await createShareUrl({
            fileIds: shareFileIds.value,
            shareDay: shareDay.value,
        })
    }

    if (res.code == 200) {
        ElMessage.success('创建成功！，请复制链接分享');
        shareUrl.value = `${window.location.origin}/share/` + res.data;
        isShareSuccess.value = true;
        return;
    }
    else {
        ElMessage.error(res.message);
    }
}
// 复制操作
const copyShareUrl = async () => {
    try {
        // 现代浏览器方案（推荐）
        if (navigator.clipboard) {
            await navigator.clipboard.writeText(shareUrl.value);
            showSuccessToast('链接已复制到剪切板');
            return;
        }
        // 兼容旧版浏览器的降级方案
        const textarea = document.createElement('textarea');
        textarea.value = shareUrl.value;
        textarea.style.position = 'fixed';  // 防止滚动
        document.body.appendChild(textarea);
        textarea.select();

        const result = document.execCommand('copy');
        document.body.removeChild(textarea);

        if (result) {
            showSuccessToast('链接已复制');
        } else {
            throw new Error('复制失败');
        }
    } catch (err) {
        console.log(err);
    }
};

// 显示成功反馈
const showSuccessToast = (message: string) => {
    // 这里可以使用 UI 库的通知组件，或简单 alert
    ElMessage.success(message);
    // 推荐使用 ElMessage 等组件：
    // ElMessage.success(message);
};

const close = () => {
    isShareSuccess.value = false;
    $bus.emit('closeSharePicture');
}
watch(() => props.show, (val) => {
    dialogVisible.value = val;
    shareFileIds.value = props.checkedImages.flat();
    shareAlbumIds.value = props.checkedAlbums;
    shareImageUrlList.value = [];
    let count = 0;
    if (props.type == 'album') {
        shareAlbumIds.value.map((item) => {
            if (count == 3) return;
            props.albumInfoList.find((albumInfo) => {
                if (albumInfo.albumId === item) {
                    shareImageUrlList.value.push(albumInfo.coverUrl);
                    count++;
                }
            })
        })
    }
    else {
        props.checkedImages.flat().map((item) => {
            if (count == 3) return;
            props.fileInfoList.forEach((fileInfo) => {
                fileInfo.fileList.find((file) => {
                    if (file.fileId === item) {
                        shareImageUrlList.value.push(file.thumbnailUrl);
                        count++;
                    }
                })
            })

        })
    }


})
watch(dialogVisible, (val) => emit("update:show", val))
</script>

<style scoped>
/* 公共样式 */
/* 基础容器 */
.share-image {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.image-container {
    position: relative;
    width: 250px;
    /* 修改 */
    height: 250px;
    /* 修改 */
    display: grid;
    gap: 5px;
    overflow: hidden;
    border-radius: 10px;
}

/* 1张图片 */
.image-container.count-1 {
    grid-template: 1fr / 1fr;
}

/* 2张图片 */
.image-container.count-2 {
    grid-template: 1fr / 146px 99px;
    /* 按比例缩放: 原175→146 (250/300 * 175) */
}

/* 3张图片 */
.image-container.count-3 {
    grid-template:
        "main sub1" 125px
        /* 原150→125 (250/300 * 150) */
        "main sub2" 125px
        / 146px 99px;
    /* 原175→146，125→99 */
}

.image-container.count-3 .el-image:nth-child(1) {
    grid-area: main;
}

.image-container.count-3 .el-image:nth-child(2) {
    grid-area: sub1;
}

.image-container.count-3 .el-image:nth-child(3) {
    grid-area: sub2;
}


/* 图片通用样式保持不变 */
.el-image {
    width: 100%;
    height: 100%;
    background: #f5f5f5;
}

.image-count {
    background: rgba(0, 0, 0, .6);
    height: 27px;
    border-radius: 13.5px;
    line-height: 27px;
    text-align: center;
    color: #fff;
    font-weight: 600;
    font-size: 16px;
    position: absolute;
    right: 8px;
    bottom: 10px;
    padding: 0 10px;
}

.share-time {

    padding: 15px;
    padding-bottom: 0;
    display: flex;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
}

.share-time-title {
    white-space: nowrap;
    margin-right: 10px;
}

.share-button {
    height: 40px;
    margin: 20px;
    width: calc(100% - 40px);
    border-radius: 20px;
}
</style>