<template>
    <div id="album">
        <!-- 顶部菜单 -->
        <div class="content-header">
            <div class="content-header-left">

                <el-button @click="dialogVisible = true" type="primary" plain round class="content-header-left-button">
                    <i-solar-album-broken />
                    <span class="content-header-button-span">创建相册</span>
                </el-button>
            </div>
            <div class="content-header-right">
                <!-- 排序 -->
                <el-dropdown trigger="click" @command="handleCommandOrderType">
                    <el-button text>
                        <i-ep-calendar />
                        <span class="content-header-button-span">{{ orderKeyword.label }}</span>
                    </el-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: orderKeyword.value == item.value ? '#409EFF' : '' }"
                                :icon="orderKeyword.value == item.value ? 'Check' : ''" :command="index"
                                v-for="(item, index) in orderKeywordList">{{
                                    item.label
                                }}</el-dropdown-item>
                            <el-divider />
                            <el-dropdown-item icon="Top" class="content-header-right-item"
                                :style="{ color: orderType.value == orderTypeList[0].value ? '#409EFF' : '' }"
                                :command="2">
                                <span>{{ orderTypeList[0].label }}</span>
                            </el-dropdown-item>
                            <el-dropdown-item icon="Bottom" class="content-header-right-item"
                                :style="{ color: orderType.value == orderTypeList[1].value ? '#409EFF' : '' }"
                                :command="3">
                                <span>{{ orderTypeList[1].label }}</span>
                            </el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <!-- 照片尺寸 -->
                <el-dropdown trigger="click" @command="handleCommandImageSize">
                    <el-button text class="content-header-right-button">
                        <i-ep-grid />
                        <span class="content-header-button-span">{{ imageStyleText }}</span>
                    </el-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: imageStyleText == item.label ? '#409EFF' : '' }"
                                :icon="imageStyleText == item.label ? Check : ''" :command="index"
                                v-for="(item, index) in imageStyleSizeList">{{ item.label
                                }}</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>
        <el-divider />

        <!-- 顶部选中菜单 -->
        <div id="checked-menu" :style="displayCheckededMenu">
            <div class="checked-menu-item">
            <el-icon class="checked-menu-icon-text close-icon">
                <i-ep:CloseBold @click="closecheckedMenu()" />
            </el-icon>
            <span class="checked-menu-icon-text checked-menu-item-select">已选择{{ checkedAlbums.length }}个相册</span>
            <el-button color="#3174ff" @click="handleCheckAll">
                <span class="icon-text">{{ isCheckAllText }}</span>
            </el-button>
        </div>
            <div class="checked-menu-feature">

                <el-button color="#3174ff" class="checked-menu-button" @click="clickDownloadAlbum"> <el-icon
                        class="icon-text">
                        <i-ep:download />
                    </el-icon><span class="icon-text">下载相册</span>
                </el-button>
                <el-button color="#3174ff" class="checked-menu-button" @click="showSharePicture = !showSharePicture;">
                    <el-icon class="icon-text">
                        <i-ep:share />
                    </el-icon><span class="icon-text">分享相册</span>
                </el-button>
                <el-button color="#3174ff" class="checked-menu-button" @click="deleteFormVisible = true"> <el-icon
                        class="icon-text">
                        <i-ep:delete />
                    </el-icon><span class="icon-text">删除相册</span>
                </el-button>
            </div>
        </div>

        <!-- 相册列表显示 -->
        <div id="picture-list" @scroll="handleScroll">
            <div class="picture-group-content">
                <div class="picture-group-album" v-for="(album, index) in albumInfoList" :key="index">
                    <div class="picture-group-item" :style="pictureSacleStyle" :class="isAddCheckArea">
                        <el-checkbox-group :style="checkBoxVisibility" class="picture-group-checkbox"
                            v-model="checkedAlbums" @change="handleCheckedAlbum">
                            <el-checkbox :key="index" :value="album.albumId" size="large">
                            </el-checkbox>
                        </el-checkbox-group>
                        <div class="picture-group-check-bg">
                        </div>
                        <div class="picture-group-number" v-if="album.imageCount + album.videoCount > 0">
                            {{ album.imageCount + album.videoCount }}
                        </div>
                        <el-image @click="handleClickAlbum(album)"
                            :src="album.coverUrl || defaultCover"
                            :class="{ 'picture-group-image': true, 'picture-group-image-animation': isImageChecked(album.albumId) }"
                            fit="cover">
                        </el-image>

                    </div>

                    <div class="picture-group-info" :style="{ width: pictureSacleStyle.width }">
                        <div class="picture-group-info-name">{{ album.albumName }}</div>
                        <div class="picture-group-info-time">{{ formatDate(album.createTime) }}</div>
                    </div>
                </div>
            </div>
            <el-empty v-if="albumInfoList.length == 0" description="暂无数据">
            </el-empty>
        </div>

        <el-dialog :modal="false" v-model="dialogVisible" title="创建相册" width="400" center @closed="albumName = ''">
            <el-input v-model="albumName" style="padding: 10px 10px 10px 10px;margin:0" placeholder="请输入相册名称"
                maxlength="30">
                <template #prepend>
                    <span style="user-select: none;">相册名</span>
                </template>

            </el-input>
            <div style="height: 10px;padding: 0px 10px 0px 10px;position: relative;"> <span
                    style="position: absolute;right: 10px;">{{ albumName.length }}/30</span></div>

            <template #footer>
                <div class="dialog-footer">

                    <el-button @click="dialogVisible = false" style="width: 80px;">取消</el-button>
                    <el-button type="primary" @click="createAlbum" style="width: 80px;">
                        确认
                    </el-button>
                </div>
            </template>
        </el-dialog>

        <el-dialog draggable :modal="false" v-model="deleteFormVisible" title="确认删除相册吗？" width="400" center>
            <div style="display: flex;align-items: center;justify-content: center;">
                <el-icon style="color: #F56C6C;font-size: 20px;margin-right:5px;vertical-align:middle;">
                    <WarningFilled />
                </el-icon>
                <span style="color: #F56C6C;">相册一经删除将无法恢复，请谨慎操作</span>
            </div>
            <div style="display: flex;align-items: center;justify-content: center;margin-top: 20px;color: #409eff;">
                <el-checkbox v-model="deleteForm.isDeletePicture" class="deleteForm" />同时将该相册内的所有图片加入回收站
            </div>
            <template #footer>
                <div class="dialog-footer">
                    <el-button type="danger" @click="onDeleteFormSubmit">确认</el-button>
                    <el-button @click="deleteFormVisible = false">取消</el-button>
                </div>
            </template>

        </el-dialog>
        <!-- 分享照片 -->
        <SharePicture type="album" v-model:show="showSharePicture" :checkedImages="[]" :fileInfoList="[]"
            :checkedAlbums="checkedAlbums" :albumInfoList="albumInfoList" />
    </div>

</template>
<script setup lang="ts">
/******************************​ 依赖导入 ​******************************/
import { onMounted, ref, reactive, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Check } from '@element-plus/icons-vue'
import type { CheckboxValueType } from 'element-plus'
import { selectAllAlbum, addAlbum, getDownloadAlbumToken, deleteAlbum } from '@/api/album/album'
import $bus from '@/utils/bus.ts'
import requestPublicConfig from "@/api/config";
import defaultCover from '@/assets/image/album.png';
/******************************​ 接口定义 ​******************************/
interface FilePage {
    records: API.albumInfo;
    total: number;
    size: number;
    current: number;
    pages: number;
}

/******************************​ 配置项 ​******************************/
// 排序配置
const orderTypeList = [
    { value: 'asc', label: '升序排列' },
    { value: 'desc', label: '降序排列' },
]
const orderKeywordList = [
    { value: 'create_time', label: '创建时间' },
    { value: 'update_time', label: '修改时间' },
]

// 图片样式配置
const imageStyleSizeList = [
    { label: '大图模式', value: 180 },
    { label: '中图模式', value: 150 },
    { label: '小图模式', value: 130 },
]
// 路由
const router = useRouter();

/******************************​ 响应式变量 ​******************************/
// 对话框相关
const dialogVisible = ref(false)
const albumName = ref('')
const showSharePicture = ref(false);
// 分页相关
const pageRequest = ref({
    current: 1,
    size: 50,
    orderKeyword: 'create_time',
    orderType: 'desc'
})
//删除相册
const deleteFormVisible = ref(false);
const deleteForm = reactive({
    albumIds: [0],
    isDeletePicture: false
})
const onDeleteFormSubmit = async () => {
    deleteForm.albumIds = checkedAlbums.value;
    deleteFormVisible.value = false;
    const res = await deleteAlbum(deleteForm);
    if (res.code == 200) {
        ElMessage.success('删除成功');
        handleSelectAllAlbum();
    }
    else {
        ElMessage.error('删除失败');
    }
    closecheckedMenu();

}
// 排序相关
const orderKeyword = ref(orderKeywordList[0])
const orderType = ref(orderTypeList[1])

// 视图样式相关
const imageStyleSize = ref(imageStyleSizeList[1].value)
const imageStyleText = ref(imageStyleSizeList[1].label)
const imageAdjustSize = ref(imageStyleSize.value)

// 加载状态
const isLoading = ref(false)
const hasMore = ref(true)

// 选择相关状态
const displayCheckededMenu = ref({ display: 'none' })
const checkBoxVisibility = ref({ display: '' })
const isAddCheckArea = ref('')
const isCheckAll = ref(false)
const isCheckAllText = ref('全选')
const checkedAlbums = ref<number[]>([])
const checkedAllAlbums = ref<number[]>([])


// 数据相关
const albumInfoList = ref<API.albumInfo[]>([])

/******************************​ 计算属性 ​******************************/
const pictureSacleStyle = computed(() => ({
    '--imageSize': imageAdjustSize.value + 'px',
    width: imageAdjustSize.value + 'px',
    height: imageAdjustSize.value + 'px',
}))

/******************************​ 生命周期钩子 ​******************************/
let observer: ResizeObserver | null = null

onMounted(() => {
    initCheckedAllAlbums()
    handleSelectAllAlbum()
    setupResizeObserver()
})

onUnmounted(() => {
    $bus.off('closeSharePicture');
    $bus.on('closeSharePicture', () => {
        closecheckedMenu();

    })
    if (observer) observer.disconnect()
})

/******************************​ 事件处理函数 ​******************************/
const handleClickAlbum = (album: API.albumInfo) => {
    router.push({
        name: 'AlbumPicture',
        params: { albumId: album.albumId }
    })
}
// 排序方式变化
const handleCommandOrderType = (index: number) => {
    if (index < 2) {
        orderKeyword.value = orderKeywordList[index]
    } else {
        orderType.value = orderTypeList[index - 2]
    }
    pageRequest.value.orderKeyword = orderKeyword.value.value
    pageRequest.value.orderType = orderType.value.value
    handleSelectAllAlbum()
}

// 图片尺寸变化
const handleCommandImageSize = (index: number) => {
    imageStyleSize.value = imageStyleSizeList[index].value
    imageStyleText.value = imageStyleSizeList[index].label
    adjustImageSizeToContainer()
}

// 创建相册
const createAlbum = () => {
    if (!albumName.value) {
        ElMessage.error('请输入相册名称')
        return
    }
    addAlbum({ albumName: albumName.value }).then(res => {
        if (res.code === 200) {
            handleSelectAllAlbum()
            ElMessage.success(res.message)
        } else {
            ElMessage.error(res.message)
        }
        dialogVisible.value = false
    })
}

// 滚动加载处理
const handleScroll = (e: Event) => {
    const container = e.target as HTMLElement
    if (container.scrollHeight - (container.scrollTop + container.clientHeight) <= 1) {
        if (!isLoading.value && hasMore.value) loadMoreData()
    }
}

// // 全选/反选处理
const handleCheckAll = () => {
    // 切换全选状态
    if (isCheckAll.value) {
        // 执行取消勾选
        closecheckedMenu();
    }
    else {
        // 执行全选
        checkedAlbums.value = checkedAllAlbums.value;
        isCheckAll.value = true;
        isCheckAllText.value = '取消勾选';
        toggleCheckUI(checkedAlbums.value.length > 0);
    }
}
// 复选框变化处理
const handleCheckedAlbum = (value: CheckboxValueType[]) => {
    const checkedCount = value.length
    isCheckAll.value = checkedCount === checkedAllAlbums.value.length
    isCheckAllText.value = isCheckAll.value ? '取消勾选' : '全选'
    toggleCheckUI(checkedAlbums.value.length > 0)
}


/******************************​ 工具函数 ​******************************/
// 日期格式化
const formatDate = (isoString: string) => {
    const date = new Date(isoString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 补零到两位数
    const day = String(date.getDate()).padStart(2, '0');         // 补零到两位数
    return `${year}-${month}-${day}`;
}

// 调整图片尺寸逻辑
const adjustImageSize = (containerWidth: number) => {
    const count = Math.max(1, Math.floor(containerWidth / (imageStyleSize.value + 20)))
    imageAdjustSize.value = Math.floor(containerWidth / count) - 20
}

// 防抖函数
const debounce = (fn: Function, delay = 100) => {
    let timer: number | null = null
    return (...args: any[]) => {
        timer && clearTimeout(timer)
        timer = setTimeout(() => fn(...args), delay)
    }
}

// 检查相册选中状态
const isImageChecked = (albumId: number) => {
    return checkedAlbums.value.some(item => item === albumId)
}

/******************************​ 数据请求 ​******************************/
const clickDownloadAlbum = async () => {
    ElMessage.info("下载开始...");
    checkedAlbums.value.forEach(async (item, index) => {
        // 获取带鉴权的临时下载令牌
        const res =  await getDownloadAlbumToken({
            albumId: item
        })
        if (res.code !== 200) {
            ElMessage.error(item+"下载失败！");
            return;
        }
        else{
            const token = res.data;
            // 构造下载链接
            const downloadUrl = requestPublicConfig.baseUrl + `/album/downloadAlbumByToken?downloadToken=${encodeURIComponent(token)}`;

            // 创建隐藏iframe触发原生下载
            const iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = downloadUrl;
            iframe.onload = () => {
                document.body.removeChild(iframe);
                closecheckedMenu();
            };
            document.body.appendChild(iframe);
            
        }
    })
}
// 获取相册列表
const handleSelectAllAlbum = () => {
    selectAllAlbum(pageRequest.value).then(res => {
        if (res.code === 200) {

            albumInfoList.value = res.data.records
            initCheckedAllAlbums()
        } else {
            ElMessage.error(res.message)
        }
    })
}

// 加载更多数据
const loadMoreData = async () => {
    isLoading.value = true
    pageRequest.value.current += 1
    try {
        const res = await selectAllAlbum(pageRequest.value)
        if (res.code === 200) {
            albumInfoList.value = [...albumInfoList.value, ...res.data.records]
            hasMore.value = pageRequest.value.current < res.data.pages
            setCheckedAllAlbums(res.data.records)
        }
    } finally {
        isLoading.value = false
    }
}

/******************************​ UI控制 ​******************************/
// 切换选择UI状态
const toggleCheckUI = (shouldShow: boolean) => {
    displayCheckededMenu.value.display = shouldShow ? 'flex' : 'none'
    checkBoxVisibility.value.display = shouldShow ? 'flex' : 'none'
    isAddCheckArea.value = shouldShow ? 'addCheckArea' : ''
}

// 关闭顶部菜单
const closecheckedMenu = () => {
    isCheckAll.value = false
    checkedAlbums.value = []
    toggleCheckUI(false)
}

/******************************​ 初始化函数 ​******************************/
// 初始化选中状态
const initCheckedAllAlbums = () => {
    checkedAllAlbums.value = albumInfoList.value.map(item => item.albumId)
}

// 设置批量选中状态
const setCheckedAllAlbums = (newAlbums: API.albumInfo[]) => {
    checkedAllAlbums.value.push(...newAlbums.map(item => item.albumId))
}

// 初始化ResizeObserver
const setupResizeObserver = () => {
    const container = document.getElementById('picture-list')
    if (!container) return

    const debouncedAdjust = debounce((width: number) => adjustImageSize(width))
    observer = new ResizeObserver(entries => {
        debouncedAdjust(entries[0].contentRect.width - 20)
    })
    observer.observe(container)
}

// 容器尺寸调整
const adjustImageSizeToContainer = () => {
    const container = document.getElementById('picture-list')
    container && requestAnimationFrame(() => adjustImageSize(container.getBoundingClientRect().width - 20))
}
</script>


<style scoped>
#album {
    width: 100%;
    height: 100%;
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

/* 左边按钮 */
.content-header-left-button {
    width: 100px;
    margin-left: 20px;
}

.content-header-right-button {
    width: 100px;
}

.content-header-menu {
    width: 160px;
}

.content-header-left {
    height: 100%;
    display: flex;
    align-items: center;
    margin-right: auto;
}

/* 右边操作 */
.content-header-right {
    height: 100%;
    margin-right: 20px;
    display: flex;
    align-items: center;
    justify-content: flex-end;

}

.content-header-button-span {
    margin-left: 5px;
}

:deep(.content-header-right-item i) {
    font-size: 16px;
}


/* 自定义ElementUI分割线 */
.el-divider--horizontal {
    margin-top: 0px;
    margin-bottom: 0px;
}

/* 自定义Element dropdown */
:deep(.content-header-right-item) {
    display: flex;
    justify-content: flex-end;
    margin: 5px;
}

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
.close-icon{
    font-size: 22px;
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
    height: calc(100% - 60px);
    overflow-y: auto;
    overflow-x: hidden;
}

/* 图片加载 */
.picture-loading {
    height: 60px;
    width: 100%;
    margin-bottom: 20px;
}

.picture-group-content {
    margin-top: 20px;
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
}

.picture-group-album {
    margin: 10px;
}

.picture-group-item {
    position: relative;
    cursor: pointer;
    border-radius: 8px;
    border: #E5E5E5 1px solid;
    /* transition: all 0.3s ease; */
    box-shadow: 12px 12px 10px -5px rgba(0, 0, 0, 0.3);

}

.picture-group-info-name {
    user-select: none;
    cursor: pointer;
    vertical-align: baseline;
    margin-left: 5px;
    margin-top: 4px;
    margin-bottom: 4px;
    font-size: 15px;
    font-weight: 700;
    line-height: 20px;

    word-break: break-all;
}

.picture-group-info-time {
    user-select: none;
    cursor: pointer;
    vertical-align: baseline;
    margin-left: 5px;
    color: #030303;
    font-size: 14px;
    word-break: break-all;
}

.picture-group-number {
    background-color: #5a5a5a;
    position: absolute;
    bottom: 10px;
    right: 10px;
    z-index: 2;
    line-height: 1;
    padding: 2px;
    border-radius: 3px;
    box-sizing: border-box;
    font-size: 18px;
    color: #fff;

}

/*图片css */
.picture-group-image {
    border-radius: 8px;
    width: 100%;
    height: 100%;
    transition: transform 0.3s;
    transform: scale(1);
}

.picture-group-image-animation {
    transition: transform 0.3s;
    transform: scale(0.9);
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
    border-radius: 8px;
    width: 100%;
    height: 30px;
    z-index: 2;
    position: absolute;
    top: 0;
}











/* 自定义element checkBox样式 */


:deep(.el-checkbox .el-checkbox__inner) {
    z-index: 3;
    border-radius: 50%;
    width: 18px;
    height: 18px;
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

:deep(.deleteForm .el-checkbox__inner) {
    border: 2px solid #d7dee2;
}

.deleteForm {
    margin-right: 5px;
}

:deep(.el-checkbox__inner::after) {
    border: 2px solid #fff;
    border-left: 0;
    border-top: 0;
    left: 5.2px;
    top: 2px;
}

:deep(.picture-group-checkAll span span::before) {
    top: 6px !important;
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
