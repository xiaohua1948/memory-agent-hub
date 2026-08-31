<template>
    <!-- 顶部选中菜单 -->
    <div id="checked-menu" :style="displayCheckededMenu">
        <div class="checked-menu-item">
            <el-icon class="checked-menu-icon-text close-icon">
                <i-ep:CloseBold @click="closecheckedMenu()" />
            </el-icon>
            <span class="checked-menu-icon-text checked-menu-item-select">已选择{{ computedCheckedCount }}张图片</span>
            <el-button color="#3174ff" @click="handleClickCheckAll">
                <span class="icon-text">{{ isCheckAllText }}</span>
            </el-button>
        </div>
        <div class="checked-menu-feature">

            <el-button color="#3174ff" class="checked-menu-button" @click="clickAddToAlbum">
                <el-icon class="checked-menu-icon">
                    <i-solar:album-bold-duotone />
                </el-icon><span class="checked-menu-icon-text">添加</span>
            </el-button>


            <el-button color="#3174ff" class="checked-menu-button" @click="clickDownloadPicture"> <el-icon
                    class="checked-menu-icon">
                    <i-ep:download />
                </el-icon><span class="checked-menu-icon-text">下载</span>
            </el-button>

            <el-button color="#3174ff" class="checked-menu-button" @click="clickSavePicture"> <el-icon
                    class="checked-menu-icon">
                    <i-famicons:cloud-upload-outline />
                </el-icon><span class="checked-menu-icon-text">保存</span>
            </el-button>
        </div>
    </div>

    <!-- 图片列表显示 -->
    <div id="picture-list" @scroll="handleEmitScroll($event.currentTarget as HTMLElement)">
        <div class="picture-group" v-for="(fileInfo, ListIndex) in fileInfoList" :key="ListIndex">

            <div class="picture-group-header">
                <el-checkbox v-model="checkAll[ListIndex]" :indeterminate="isIndeterminate[ListIndex]"
                    @change="(val: any) => handleCheckAllChange(val, ListIndex)" size="large"
                    :style="checkBoxVisibility" class="picture-group-checkAll">
                </el-checkbox>
                <span :style="pictureGroupTime" class="picture-group-time">{{ formatDate(fileInfo.time) }}</span>
            </div>

            <div class="picture-group-content">
                <div class="picture-group-item" :class="isAddCheckArea" :style="pictureSacleStyle(file)"
                    v-for="(file, fileIndex) in fileInfo.fileList" :key="fileIndex">
                    <el-checkbox-group :style="checkBoxVisibility" class="picture-group-checkbox"
                        v-model="checkedImages[ListIndex]"
                        @change="(e: string[]) => handleCheckedImagesChange(e, ListIndex)">
                        <el-checkbox :value="file.fileId" size="large">
                        </el-checkbox>
                    </el-checkbox-group>
                    <div :style="clickedCheckBgStyle" class="picture-group-check-bg">
                    </div>
                    <VideoOrImagePreview :is-image="file.category === 'image'"
                        :video-time="formatDuration(file.duration)" :isImageChecked="isImageChecked(file.fileId)"
                        :showDrawer="showDrawer" :fileId-list="checkedAllImages.flat()" :src="file.thumbnailUrl"
                        :preview-src-list="previewImageList" :initial-index="getPreviewImageIndex(ListIndex, fileIndex)"
                        class="picture-group-image" @click="() => { currentFileId = file.fileId; }">
                        <!-- 插槽：扩展element图片预览功能 -->
                        <template #viewer>
                            <div class="my-image-viewer-action">
                                <div class="my-image-viewer-action-inner">
                                    <el-tooltip content="下载图片" placement="top" :show-after="300">
                                        <i-ep:download class="my-image-viewer-icon" @click="clickDownloadPicture" />
                                    </el-tooltip>

                                    <el-tooltip content="添加相册" placement="top" :show-after="300">
                                        <i-solar:album-bold-duotone class="my-image-viewer-icon"
                                            @click="clickAddToAlbum" />
                                    </el-tooltip>
                                    <el-tooltip content="保存云端" placement="top" :show-after="300">
                                        <i-famicons:cloud-upload-outline class="my-image-viewer-icon"
                                            @click="clickSavePicture" />
                                    </el-tooltip>
                                    <el-tooltip content="照片详情" placement="top" :show-after="300">
                                        <i-ep:Warning class="my-image-viewer-icon"
                                            @click="() => { showDrawer = !showDrawer; }" />
                                    </el-tooltip>

                                </div>
                            </div>
                        </template>


                    </VideoOrImagePreview>
                </div>
            </div>
        </div>
        <el-empty v-if="fileInfoList.length == 0" description="暂无数据">
        </el-empty>

    </div>


    <!-- 添加相册 -->
    <AddAlbum v-model:show="showAddAlbumList" :checked-images="checkedImages" :currentFileId="currentFileId" />





</template>
<script setup lang="ts">
/******************************​ 依赖导入 ​******************************/
import { onMounted, ref, computed, watch, onUnmounted, nextTick } from 'vue'
import $bus from '@/utils/bus.ts'
import VideoOrImagePreview from '@/components/preview/VideoOrImagePreview.vue'
import AddAlbum from '@/components/album/AddAlbum.vue'
import requestPublicConfig from '@/api/config'
import { getDownloadSharedFileToken, saveSharePicture } from '@/api/file/file'
/******************************​ 接口定义 ​******************************/

interface Props {
    parentComponent: string,
    fileInfoList: API.FileInfoList[],
    moreFileInfoList: API.FileInfoList[],
    albumId: number,
    imageStyleSize: number,
    scale: { value: string; label: string },
    shareToken: string | string[],
}

/******************************​ Props 配置 ​******************************/
const props = withDefaults(defineProps<Props>(), {
    parentComponent: 'AllPicture',
    fileInfoList: () => [],
    moreFileInfoList: () => [],
    albumId: -1,
    imageStyleSize: 150,
    scale: () => ({ value: 'original', label: '原始比例' }),
    shareToken: '',
})

/******************************​ 响应式变量 ​******************************/



// 选中背景样式控制
const clickedCheckBgStyle = ref('')
// 视图相关
const imageStyleSize = ref(props.imageStyleSize)
const fileInfoList = ref<API.FileInfoList[]>([])
const previewImageList = ref<string[]>([])


// 选择相关
const displayCheckededMenu = ref({ display: 'none' })
const checkBoxVisibility = ref({ display: '' })
const isAddCheckArea = ref('')
const isCheckAll = ref(false)
const isCheckAllText = ref('全选')
const checkedImages = ref<string[][]>([[]])
const checkedAllImages = ref<string[][]>([[]])
const checkAll = ref<boolean[]>([])
const isIndeterminate = ref<boolean[]>([])

// DOM 相关
let observer: ResizeObserver | null = null
const showDrawer = ref(false)
// 展示添加相册列表
const showAddAlbumList = ref(false);
const showSharePicture = ref(false);
const showPictureTag = ref(false);

//当前文件id，用于点击图片后执行请求
const currentFileId = ref('');
const pictureGroupTime = ref({ transform: 'translateX(0px)' })

/******************************​ 计算属性 ​******************************/
const computedCheckedCount = computed({
    get() {
        return checkedImages.value.flat().length
    },
    set(val: number) { return val }
})

const pictureSacleStyle = (file: API.FileInfo) => {
    let adjustWidth = 0;
    if (props.scale.value === 'original') {
        adjustWidth = file.width / file.height * imageStyleSize.value
       
    }

    return props.scale.value === 'original'
        ? {
            '--imageSize': imageStyleSize.value + 'px',
            'flex-grow': adjustWidth,
            width: adjustWidth + 'px',
            'max-width': adjustWidth + 80 + 'px',
            height: imageStyleSize.value + 'px'
        }
        : {
            '--imageSize': imageStyleSize.value + 'px',
            width: imageStyleSize.value + 'px',
            height: imageStyleSize.value + 'px'
        }
}
// 组件通信
const showBus = () => {
    // 先清除之前的监听事件,进入相册后修改当前的相册Id
    $bus.off('showDrawer');
    $bus.on('showDrawer', () => {
        showDrawer.value = !showDrawer.value
    })
    $bus.off('closeAddAlbum');
    $bus.on('closeAddAlbum', () => {
        showAddAlbumList.value = false
        closecheckedMenu();
    })
    $bus.off('setCurrentFileId');
    $bus.on('setCurrentFileId', (val: any) => {

        currentFileId.value = val;
    })
}
/******************************​ 生命周期钩子 ​******************************/
onMounted(() => {
    showBus();
    setupResizeObserver()
})

onUnmounted(() => {
    observer?.disconnect()
})

/******************************​ 事件处理函数 ​******************************/
// 滚动加载数据
const handleEmitScroll = (container: HTMLElement) => {
    if (container.scrollHeight - (container.scrollTop + container.clientHeight) <= 1) {
        $bus.emit('loadMoreData')
    }
}



// 全选/反选处理
const handleClickCheckAll = () => {
    if (isCheckAll.value) {
        closecheckedMenu()
    } else {
        isCheckAll.value = true
        isCheckAllText.value = '取消勾选'
        checkedAllImages.value.forEach((_, index) => {
            checkedImages.value[index] = checkedAllImages.value[index]
            checkAll.value[index] = true
        })
    }
    isIndeterminate.value.fill(false)
}

// 分组全选状态变化
const handleCheckAllChange = (val: boolean, groupIndex: number) => {
    checkAll.value[groupIndex] = val
    checkedImages.value[groupIndex] = val ? checkedAllImages.value[groupIndex] : []
    isIndeterminate.value[groupIndex] = false
    toggleCheckUI(computedCheckedCount.value > 0)
}

// 单个图片选中状态变化
const handleCheckedImagesChange = (val: string[], groupIndex: number) => {
    const length = val.length
    checkAll.value[groupIndex] = length === checkedAllImages.value[groupIndex].length
    isIndeterminate.value[groupIndex] = length > 0 && length < checkedAllImages.value[groupIndex].length
    toggleCheckUI(computedCheckedCount.value > 0)
}


//检查登录
const checkLogin = () => {
    let token = localStorage.getItem('token');
    if (!token) {
        ElMessage.warning('请先登录');
        return false;
    }
    return true;
}

// 点击添加相册
const clickAddToAlbum = () => {

    if (!checkLogin()) return;
    if (currentFileId.value.length == 32) {
    }

    showAddAlbumList.value = true;
}
// 点击保存图片
const clickSavePicture = async () => {
    if (!checkLogin()) return;
    let checkfileIds = [''];
    //点击复选框选中的图片发送请求数据
    if (currentFileId.value.length < 32) {
        checkfileIds = checkedImages.value.flat();
    }
    else {
        //点击单个图片请求数据
        checkfileIds = [currentFileId.value];
    }
    const res = await saveSharePicture({
        fileIds: checkfileIds,
        shareToken: props.shareToken as string,
    })
    if (res.code == 200) {
        ElMessage.success('保存成功！');
    }

}

//点击下载图片

const clickDownloadPicture = async () => {
    if (!checkLogin()) {
        return;
    }

    try {
        // 获取选中的文件ID（保持原有逻辑）
        let checkfileIds: string[] = [];
        if (currentFileId.value.length < 32) {
            checkfileIds = checkedImages.value.flat();
        } else {
            checkfileIds = [currentFileId.value];
        }

        // 获取带鉴权的临时下载令牌
        const res = await getDownloadSharedFileToken({
            shareToken: props.shareToken,
            fileIds: checkfileIds
        })// Sa-Token会自动携带cookie/storage中的token
        if (res.code !== 200) {
            ElMessage.error("下载失败！");
            return;
        }
        else {

            const token = res.data;
            // 构造下载链接
            const downloadUrl = requestPublicConfig.baseUrl + `/file/downloadFileByToken?downloadToken=${encodeURIComponent(token)}`;

            // 创建隐藏iframe触发原生下载
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = downloadUrl;

            iframe.onload = () => {
                document.body.removeChild(iframe);
                closecheckedMenu();
                ElMessage.success('下载开始');
            };
            document.body.appendChild(iframe);

        }

    } catch (error) {
        ElMessage.error('下载失败');
    }
}




/******************************​ 数据请求 ​******************************/



/******************************​ UI 控制 ​******************************/
// 切换选择UI状态
const toggleCheckUI = (shouldShow: boolean) => {
    currentFileId.value = '-1';
    displayCheckededMenu.value.display = shouldShow ? 'flex' : 'none'
    checkBoxVisibility.value.display = shouldShow ? 'flex' : 'none'
    pictureGroupTime.value.transform = shouldShow ? 'translateX(12px)' : 'translateX(0px)'
    isAddCheckArea.value = shouldShow ? 'addCheckArea' : ''
    isCheckAll.value = checkAll.value.every(Boolean)
    isCheckAllText.value = isCheckAll.value ? '取消勾选' : '全选'
}

// 关闭顶部菜单
const closecheckedMenu = () => {
    toggleCheckUI(false)
    checkedImages.value = []
    checkAll.value.fill(false)
    isIndeterminate.value.fill(false)
}

/******************************​ 初始化函数 ​******************************/
// 初始化选择状态
const initCheckedAllImages = () => {
    checkedAllImages.value = fileInfoList.value.map(item =>
        item.fileList.map(file => file.fileId)
    )
    checkAll.value = Array(fileInfoList.value.length).fill(false)
    isIndeterminate.value = Array(fileInfoList.value.length).fill(false)
}

// 设置批量选择状态
const setCheckedAllImages = (newFiles: API.FileInfoList[]) => {
    checkedAllImages.value.push(...newFiles.map(item =>
        item.fileList.map(file => file.fileId)
    ))
    checkAll.value.push(...Array(newFiles.length).fill(false))
    isIndeterminate.value.push(...Array(newFiles.length).fill(false))
}

// 初始化预览列表
const initPreviewImageList = () => {
    previewImageList.value = fileInfoList.value.flatMap(item =>
        item.fileList.map(file => file.fileUrl)
    )
}

// 初始化ResizeObserver
const setupResizeObserver = () => {
    const container = document.getElementById('picture-list')
    if (!container) return

    const debouncedAdjust = debounce(adjustImageSize)
    observer = new ResizeObserver(entries => {
        debouncedAdjust(entries[0].contentRect.width)
    })
    observer.observe(container)
}

/******************************​ 属性监听 ​******************************/
watch(
    [
        () => props.imageStyleSize,
        () => props.fileInfoList,
        () => props.moreFileInfoList,
    ],
    async ([newStyleSize, newFileInfoList, newMoreFileInfoList], [oldStyleSize, oldFileInfoList, oldMoreFileInfoList]) => {

        if (newStyleSize !== oldStyleSize) {
            imageStyleSize.value = newStyleSize
            await nextTick()
            const container = document.getElementById('picture-list')
            container && requestAnimationFrame(() =>
                adjustImageSize(container.getBoundingClientRect().width - 20)
            )
        }
        const isInitial = (newFileInfoList !== oldFileInfoList) && (newFileInfoList.length > 0);
        const isMore = (newMoreFileInfoList !== oldMoreFileInfoList) && (newMoreFileInfoList.length > 0);
        if (isInitial) {
            fileInfoList.value = newFileInfoList
            if (!isMore) {

                initPreviewImageList()
                initCheckedAllImages()
            }
        }
        if (isMore) {
            initPreviewImageList()
            setCheckedAllImages(newMoreFileInfoList)
        }
    },
    { deep: true, immediate: true }
)

/******************************​ 工具函数 ​******************************/
// 解析文件名
const parseFileNameFromDisposition = (disposition: string) => {
    if (!disposition) return "unknown_file";

    // 分割参数并去除空格（网页1、网页2的逻辑综合）
    const params = disposition.split(';').map(p => p.trim());
    let filenameParam = params.find(p => p.startsWith('filename='));

    // 处理文件名参数（网页4提到的编码问题）
    if (filenameParam) {
        // 提取值并去除首尾引号（兼容 filename="xxx" 格式）
        const encodedValue = filenameParam.split('=')[1].replace(/^["']|["']$/g, '');
        // 替换 + 号为空格（处理 URL 编码中的空格问题）
        const decodedValue = decodeURIComponent(encodedValue.replace(/\+/g, ' '));
        return decodedValue;
    }

    return "unknown_file";
}

// 日期格式化
const formatDate = (isoString: string) => {
    const date = new Date(isoString)
    const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${weekdays[date.getDay()]}`
}
// 时长格式化
const formatDuration = (seconds: number | null) => {
    if (!seconds) return '00:00'
    seconds = Math.floor(seconds)

    // 计算各部分时间
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const remainingSeconds = seconds % 60

    // 根据时长决定格式
    if (hours > 0) {
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
    }

    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}



// 防抖函数
const debounce = (fn: Function, delay = 100) => {
    let timer: number | null = null
    return (...args: any[]) => {
        timer && clearTimeout(timer)
        timer = setTimeout(() => fn(...args), delay)
    }
}

// 获取预览图片索引
const getPreviewImageIndex = (x: number, y: number) => {
    let count = 0
    for (let i = 0; i < x; i++) {
        count += fileInfoList.value[i].fileList.length
    }
    return count + y
}
// 尺寸调整逻辑
const adjustImageSize = (containerWidth: number) => {
    const count = Math.max(1, Math.floor(containerWidth / (props.imageStyleSize + 10)))
    imageStyleSize.value = Math.floor(containerWidth / count) - 10
}

// 检查图片选中状态
const isImageChecked = (imageId: string) => {
    return checkedImages.value.some(group => group.includes(imageId))
}

</script>

<style scoped>
/* 顶部选中菜单 */
#checked-menu {
    background-color: #3174ff;
    height: 60px;
    width: 100%;
    z-index: 10;
    position: absolute;
    top: 0;
    display: flex;
    align-items: center;

}

.checked-menu-item {
    margin-left: 20px;
    display: flex;
    align-items: center;
    margin-right: auto;
}

.checked-menu-item-select {
    margin-left: 10px;
    width: 120px;
}

.checked-menu-feature {
    display: flex;
    align-items: center;
    margin-right: 20px;
}

.checked-menu-button {
    padding: 5px;
}

.checked-menu-icon {
    font-size: 1.1rem;
}

.checked-menu-icon-text {
    font-size: 1rem;
    color: white;
}

.close-icon:hover {
    background-color: #ca4646;
    border-radius: 50%;
    cursor: pointer;
}

/* 图片列表 */

#picture-list {
    margin-left: 20px;
    width: calc(100% - 20px);
    height: calc(100% - 80px);
    overflow-y: auto;
    overflow-x: hidden;
}

.picture-group {
    margin-top: 10px;
}

/* 图片加载 */
.picture-loading {
    height: 60px;
    width: 100%;
    margin-bottom: 20px;
}

.picture-group-header {
    padding-left: 5px;
    width: calc(100% - 5px);
    height: 30px;
    display: flex;
    align-items: center;
}


/* 复选框隐藏时 */
.picture-group-checkAll {
    display: none;
}

/* 时间 */
.picture-group-time {
    user-select: none;
    font-size: 13px;
    color: #333;
    line-height: 20px;
    transition: 0.5s ease;
    white-space: nowrap;
}

/* 复选框显示时的时间动画 */
.picture-group:hover .picture-group-time {
    transform: translateX(12px) !important;
    /* 根据复选框宽度调整 */
}

.picture-group:hover .picture-group-checkAll {
    display: flex !important;
}

.picture-group-content {
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;

}
/* 图片瀑布流实现新增 */
.picture-group-content::after {
    content: "";
    flex: auto;
    min-width: 80%;
}

.picture-group-item {
    margin: 5px;

    position: relative;
    cursor: pointer;
    background: #e5eeff;
    transition: all 0.5s ease;
}



/* 图片选中css */
.picture-group-checkbox {
    position: absolute;
    display: none;
    margin-left: 12px;
}

.picture-group-item:hover .picture-group-check-bg {
    opacity: 0.4;
    background-image: linear-gradient(180deg, #000, transparent);
}

.picture-group-item:hover .picture-group-checkbox {
    display: flex !important;
}

/* 图片：hover后有阴影 */
.picture-group-check-bg {
    width: 100%;
    height: 30px;
    z-index: 2;
    position: absolute;
    top: 0;
}











/* 自定义element checkBox样式 */
/* !important：build 后 element-plus 按需样式 chunk（.el-checkbox--large 的 14×14 规则，特异性同为 0,3,0）
   会在本组件 CSS 之后注入并反超 18×18，容器缩回 14×14 导致对号偏移，故加 !important 固定尺寸 */
:deep(.el-checkbox .el-checkbox__inner) {
    z-index: 3;
    border-radius: 50%;
    width: 18px !important;
    height: 18px !important;
}


/* 时间标题的check全选css样式 */
:deep(.picture-group-checkAll span span) {
    border: 2px solid #aba5a5;
}

/* 设置未选中状态下的背景透明 */
:deep(.el-checkbox__inner) {
    background-color: transparent;
    border: 2px solid #fff;
}

:deep(.el-checkbox__inner::after) {
    border: 2px solid #fff;
    border-left: 0;
    border-top: 0;
    left: 5px;
    top: 2px;
}

:deep(.picture-group-checkAll span span::before) {
    top: 6px !important;
}


/* 扩充element 预览图片preview-image功能样式 */
.my-image-viewer-action {
    position: absolute;
    background-color: #606266;
    border-color: #fff;
    border-radius: 22px;
    top: 30px;
    height: 44px;
    left: 50%;
    padding: 0 23px;
    transform: translateX(-50%);
    width: 282px;
    align-items: center;
    box-sizing: border-box;
    cursor: pointer;
    display: flex;
    justify-content: center;
    opacity: 0.8;
    position: absolute;
}

.my-image-viewer-action-inner {
    align-items: center;
    color: #fff;
    display: flex;
    font-size: 23px;
    height: 100%;
    justify-content: space-around;
    width: 100%
}

/* 取消自定义icon轮廓(提示组件) */
.my-image-viewer-icon {
    outline: unset
}
</style>
<style>
/* 绑定样式isAddCheckArea 图片的check css样式:生成伪元素,扩充点击范围 */
.addCheckArea span span::before {
    content: "";
    position: absolute;
    top: -13.3px;
    left: -13.3px;

    width: var(--imageSize);
    height: var(--imageSize);
    cursor: pointer;
    z-index: 3;
}
</style>
