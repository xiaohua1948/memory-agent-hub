<template>
    <div id="album">
        <!-- 顶部菜单 -->
        <div class="content-header">
            <div class="content-header-left">
                <span class="content-header-left-span1">地点：</span>
                <span class="content-header-left-span2">你一共在<span style="color: #3b75ff;">{{ locationAlbumList.length
                        }}</span>个地方留下足迹</span>
            </div>
            <div class="content-header-right">
                <el-dropdown trigger="click" @command="handleCommandLocation">
                    <el-button text class="content-header-right-button">
                        <i-ep-location />
                        <span class="content-header-button-span">{{ locationText }}</span>
                    </el-button>
                    <template #dropdown>
                        <el-dropdown-menu style="width: 110px;">
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: locationText == item.label ? '#409EFF' : '' }"
                                :icon="locationText == item.label ? Check : ''" :command="index"
                                v-for="(item, index) in locationList">{{ item.label
                                }}</el-dropdown-item>
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



        <!-- 相册列表显示 -->
        <div id="picture-list" @scroll="handleScroll">
            <div class="picture-group-content">
                <div class="picture-group-album" v-for="(item, index) in locationAlbumList" :key="index">
                    <div class="picture-group-item" :style="pictureSacleStyle"
                        @click="handleClickLocationAlbum(item.locationLevel, item.locationValue)">
                        <el-image :src="item.coverUrl || 'src/assets/image/album.png'" fit="cover"
                            class="picture-group-image">
                        </el-image>
                        <div class="picture-group-shadow"></div>
                        <div class="picture-group-desc">

                            <div class="picture-desc-location">{{ item.locationValue }}</div>
                            <div class="picture-desc-line"></div>
                            <div class="picture-desc-count">{{ item.total }}张照片</div>

                        </div>

                    </div>


                </div>
            </div>

            <el-empty v-if="locationAlbumList.length == 0" description="暂无数据">
            </el-empty>
        </div>




    </div>

</template>
<script setup lang="ts">
/******************************​ 依赖导入 ​******************************/
import { onMounted, ref, computed, watch, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Check } from '@element-plus/icons-vue'
import { selectAllLocationAlbum } from '@/api/album/album'

/******************************​ 接口定义 ​******************************/
interface FilePage {
    records: API.albumInfo;
    total: number;
    size: number;
    current: number;
    pages: number;
}

/******************************​ 配置项 ​******************************/
const locationList = [{
    label: '国家',
    value: 'country'
}, {
    label: '省/直辖市',
    value: 'province'
}, {
    label: '市/区',
    value: 'city'
}, {
    label: '区/县',
    value: 'district'
}
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

// 分页相关
const pageRequest = ref({
    current: 1,
    size: 100,
    locationLevel: 'city',//country/provice/city/district
})

// 视图样式相关
const locationText = ref(locationList[2].label)
const imageStyleSize = ref(imageStyleSizeList[1].value)
const imageStyleText = ref(imageStyleSizeList[1].label)
const imageAdjustSize = ref(imageStyleSize.value)

// 加载状态
const isLoading = ref(false)
const hasMore = ref(true)

// 选择相关状态
const pictureSacleStyle = computed(() => ({
    width: imageAdjustSize.value + 'px',
    height: imageAdjustSize.value + 'px',
}))


// 数据相关
const locationAlbumList = ref<API.locationAlbum[]>([])


/******************************​ 生命周期钩子 ​******************************/
let observer: ResizeObserver | null = null

onMounted(() => {
    handleselectAllLocationAlbum()
    setupResizeObserver()
})

onUnmounted(() => {
    if (observer) observer.disconnect()
})

/******************************​ 事件处理函数 ​******************************/
const handleClickLocationAlbum = (locationLevel: string, locationValue: string) => {
  
    router.push({
        name: 'LocationAlbum',
        params: { locationLevel: locationLevel, locationValue: locationValue }
    })
}


// 图片尺寸变化
const handleCommandImageSize = (index: number) => {
    imageStyleSize.value = imageStyleSizeList[index].value
    imageStyleText.value = imageStyleSizeList[index].label
    adjustImageSizeToContainer()
}
// 地点选择
const handleCommandLocation = (index: number) => {
    locationText.value = locationList[index].label
    pageRequest.value.locationLevel = locationList[index].value
    handleselectAllLocationAlbum()
}



// 滚动加载处理
const handleScroll = (e: Event) => {
    const container = e.target as HTMLElement
    if (container.scrollHeight - (container.scrollTop + container.clientHeight) <= 1) {
        if (!isLoading.value && hasMore.value) loadMoreData()
    }
}





/******************************​ 工具函数 ​******************************/


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



/******************************​ 数据请求 ​******************************/
// 获取相册列表
const handleselectAllLocationAlbum = () => {
    selectAllLocationAlbum(pageRequest.value).then(res => {
        if (res.code === 200) {
           
            locationAlbumList.value = res.data.records

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
        const res = await selectAllLocationAlbum(pageRequest.value)
        if (res.code === 200) {
            locationAlbumList.value = [...locationAlbumList.value, ...res.data.records]
            hasMore.value = pageRequest.value.current < res.data.pages

        }
    } finally {
        isLoading.value = false
    }
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
    user-select: none;
}

/* 左边文字 */
.content-header-left-span1 {
    width: 50px;
    margin-left: 20px;
    font-size: large;
    white-space: nowrap;
    font-size: 20px;
    font-weight: 700;
}

.content-header-left-span2 {
    margin-left: 12px;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
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



/* 图片列表 */
#picture-list {
    margin-left: 20px;
    width: calc(100% - 20px);
    height: calc(100% - 60px);
    overflow-y: auto;
    overflow-x: hidden;
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
    transition: all 0.3s ease;

}

.picture-group-item:hover {

    box-shadow: 12px 12px 10px -5px rgba(0, 0, 0, 0.3);
}

.picture-group-image {

    width: 100%;
    height: 100%;

}

.picture-group-shadow {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: .15;
    background: #000;
}

.picture-group-desc {

    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    color: #fff;
    position: absolute;
    top: 0;
    left: 0;
}

.picture-desc-location {
    font-family: PingFangSC-Semibold;
    font-size: 16px;
    white-space: nowrap;
    text-overflow: ellipsis;
    max-width: 162px;
    overflow: hidden;
}

.picture-desc-line {
    margin-top: 3px;
    margin-bottom: 2px;
    width: 14px;
    height: 1px;
    background: #fff;
}

.picture-desc-count {
    font-family: PingFangSC-Regular;
    font-size: 12px;
}
</style>
