<template>
    <!-- 由于组件传值是只读属性，所以在关闭后需要回调函数，传回关闭状态 -->
    <el-dialog :append-to-body="true" :modelValue="props.show" :close-on-click-modal="false" :modal="false"
        title="将该项目添加" width="400" @closed="close" center>
        <span style="font-size: medium;">共{{ page.total }}个相册</span>
        <div class="album-content">
            <div class="album-item" v-for="(album) in albumInfoList" @click="handleAddAlbum(album.albumId)">
                <el-image class="album-item-image" :src="album.coverUrl || defaultCover" fit="cover" />
                <div class="album-item-desc">
                    <div class="album-item-title">{{ album.albumName }}</div>
                    <div class="album-item-time">{{ formatDate(album.createTime) }}</div>
                </div>
            </div>
        </div>

    </el-dialog>
</template>
<script lang="ts" setup>
import { ref, onMounted, watch } from 'vue'
import { selectAllAlbum, addPictureToAlbum } from '@/api/album/album'
import defaultCover from '@/assets/image/album.png';
import $bus from '@/utils/bus.ts'
interface Props {
    show: boolean,
    checkedImages: string[][],
    currentFileId: string,
}
const props = withDefaults(defineProps<Props>(), {
    show: false,
    checkedImages: () => [[]],
    currentFileId: '',
})

const page = ref({
    current: 1,
    size: 50,
    total: 0,
})
const fileIds = ref<String[]>([

])
const albumInfoList = ref<API.albumInfo[]>([

]);
watch(() => props.show, () => {

    fileIds.value = [];

    props.checkedImages.forEach((item) => {
        item.forEach((fileId) => {
            fileIds.value.push(fileId);
        })
        // 如果没有选择图片，则就添加当前图片
    })
    if (fileIds.value.length == 0) {
        fileIds.value.push(props.currentFileId);
    }
    if (props.show) {
        handleSelectAllAlbum();
    }
})
onMounted(() => {

})
const handleAddAlbum = (albumId: number) => {
    addPictureToAlbum({
        albumId: albumId,
        fileIds: fileIds.value
    }).then((res) => {
        if (res.code == 200) {
            ElMessage.success("添加成功");

        }
        else {
            ElMessage.error(res.message);
        }
        close();
    })
}
// 初始化文件(图片或视频的链接)
const handleSelectAllAlbum = () => {
    selectAllAlbum(page.value).then((res) => {

        if (res.code == 200) {
            page.value.total = res.data.total;
            page.value.size = res.data.size;
            page.value.current = res.data.current;
            albumInfoList.value = res.data.records;
        }
        else {
            ElMessage.error(res.message);
        }
    })
}
const formatDate = (isoString: string) => {
    const date = new Date(isoString);
    const year = date.getFullYear();
    const month = date.getMonth() + 1; // 月份从0开始需要+1
    const day = date.getDate();
    return `${year}年${month}月${day}日`;
}

const close = () => {
    $bus.emit("closeAddAlbum", false);
    // 修改完不能立马回传子组件  
}
</script>
<style scoped>
.album-content {
    height: 360px;
    overflow-y: auto;
    margin-top: 10px;
}

.album-item {
    height: 60px;
    display: flex;
    align-items: center;
    margin-top: 8px;
    position: relative;
    cursor: pointer;
    border-radius: 5px;

}

.album-item:hover {
    border-radius: 8px;
    background-color: rgba(59, 117, 255, .06);
}

.album-item-desc {
    height: 60px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: flex-start;
    margin-left: 10px;
}

.album-item-image {
    width: 60px;
    height: 60px;
    border-radius: 5px;
}

.album-item-title {
    font-weight: 500;
    font-size: 15px;
    color: #333;
    margin-bottom: 5px;
}

.album-item-time {

    font-size: 12px;
    color: #999;
}
</style>