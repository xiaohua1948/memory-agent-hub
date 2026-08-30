<template>
    <div id="picture" class="content">

        <!-- 顶部菜单 -->
        <div class="content-header">

            <div class="content-header-left">

                <i-ep-ArrowLeftBold style="margin-left: 30px;font-size: 25px;cursor: pointer;color: rgb(0 0 0 / 65%);"
                    @click="router.push('/search')" />

            </div>


            <div class="content-header-right">

                <!-- 照片类型 -->
                <el-dropdown trigger="click" @command="handleCommandImageType">
                    <el-button text>

                        <i-ep-camera v-if="imageTypeText.value == 'all'" />
                        <i-ep-picture v-if="imageTypeText.value == 'picture'" />
                        <i-ep-videoCamera v-if="imageTypeText.value == 'video'" />
                        <i-fluent:gif-16-regular v-if="imageTypeText.value == 'gif'" />
                        <span class="content-header-button-span">{{ imageTypeText.label }}</span>
                    </el-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: imageTypeText.label == item.label ? '#409EFF' : '' }"
                                :icon="imageTypeText.label == item.label ? Check : ''" :command="index"
                                v-for="(item, index) in imageTypeList">{{ item.label
                                }}</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
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
                        <span class="content-header-button-span">{{ imageStyle.label }}</span>
                    </el-button>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: imageStyle.label== item.label ? '#409EFF' : '' }"
                                :icon="imageStyle.label == item.label ? Check : ''" :command="index"
                                v-for="(item, index) in imageStyleSizeList">{{ item.label
                                }}</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <!-- 照片比例 -->
                <el-tooltip :content="scale.label" placement="bottom">
                    <el-button text @click="handleSacle">
                        <i-carbon:fit-to-height v-if="scale.value == 'original'" />
                        <i-carbon:fit-to-width v-else />
                    </el-button>
                </el-tooltip>
            </div>
        </div>
        <el-divider />
        <!-- 图片列表 -->
        <div class="search-keyword">{{ route.query.searchKeyword }}</div>

        <SearchPictureList parentComponent="AllPicture" :fileInfoList="fileInfoList"
            :moreFileInfoList="moreFileInfoList" :imageStyleSize="imageStyle.value" :scale="scale" :albumId="-1">
        </SearchPictureList>

    </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import $bus from '@/utils/bus.ts'
import { selectFileListByKeyword } from '@/api/file/file'
import SearchPictureList from '@/components/search/SearchPictureList.vue'
import { useRouter, useRoute } from 'vue-router';
import { storage } from '@/utils/storage';
interface FilePage {
    records: API.FileInfoList;
    total: number;
    size: number;
    current: number;
    pages: number;
}
const router = useRouter();
const route = useRoute();

/*选中菜单信息*/


onMounted(() => {
     //获取当前页面设置信息
     CURRENT_PAGE_GET();
    showBus();
    handleSelectAllFileInfo();
});
onUnmounted(() => {
    $bus.off('loadMoreData');
    $bus.off('deletePictureSuceess');
    $bus.off('removePictureSuceess');
})


/*-----------当前页面设置------------*/
interface OptionString {
  label: string;
  value: string;
}
interface OptionNumber {
  label: string;
  value: number;
}
interface CurrentPageSet {
  imageTypeText: OptionString;
  orderKeyword: OptionString;
  orderType: OptionString;
  imageStyle: OptionNumber;
  scale: OptionString;
}
// 1.图片类型方式
const imageTypeList: OptionString[] = [
  {
    label: '全部',
    value: 'all',
  },
  {
    label: '图片',
    value: 'picture',
  },
  {
    label: '视频',
    value: 'video',
  },
  {
    label: '动图',
    value: 'gif',
  },
]
// 2.排序方式、排序关键词
const orderTypeList: OptionString[] = [
  {
    value: 'asc',
    label: '升序排列',
  },
  {
    value: 'desc',
    label: '降序排列',
  },
]
const orderKeywordList: OptionString[] = [
  {
    value: 'upload_time',
    label: '上传时间',
  },
  {
    value: 'date_time_original',
    label: '拍摄时间',
  },
]

//3.图片大小模式
const imageStyleSizeList: OptionNumber[] = [
  {
    label: '大图模式',
    value: 200,
  },
  {
    label: '中图模式',
    value: 150,
  },
  {
    label: '小图模式',
    value: 120,
  },
]
//4.图片比例模式
const scale = ref<OptionString>({
  label: "正方形",
  value: "original"
});
// 默认方式
//1.图片类型方式
const imageTypeText = ref(imageTypeList[0]);
//2.排序方式、排序关键词
const orderKeyword = ref(orderKeywordList[1]);
const orderType = ref(orderTypeList[1]);
//3.图片大小模式
const imageStyle = ref(imageStyleSizeList[1]);

const CURRENT_PAGE_SET = () => {
  storage.set<CurrentPageSet>('CURRENT_PAGE_SET1', {
    imageTypeText: imageTypeText.value,
    orderKeyword: orderKeyword.value,
    orderType: orderType.value,
    imageStyle: imageStyle.value,
    scale: scale.value,
  });
}
const CURRENT_PAGE_GET = () => {
  const currentPageSet = storage.get<CurrentPageSet>('CURRENT_PAGE_SET1');
  if (currentPageSet && currentPageSet.imageTypeText && currentPageSet.orderKeyword && currentPageSet.orderType && currentPageSet.imageStyle && currentPageSet.scale) {
    imageTypeText.value = currentPageSet.imageTypeText;
    orderKeyword.value = currentPageSet.orderKeyword;
    orderType.value = currentPageSet.orderType;
    imageStyle.value = currentPageSet.imageStyle;
    scale.value = currentPageSet.scale;
    pageRequest.value.imageTypeText = imageTypeText.value.value;
    pageRequest.value.orderKeyword = orderKeyword.value.value;
    pageRequest.value.orderType = orderType.value.value;
    
  }
}
//1.修改图片类型方式
const handleCommandImageType = (index: number) => {
  imageTypeText.value = imageTypeList[index];
  pageRequest.value.imageTypeText = imageTypeText.value.value;
  handleSelectAllFileInfo();
  // 保存当前页面设置
  CURRENT_PAGE_SET();
}
//2.修改排序方式、排序关键词
const handleCommandOrderType = (index: number) => {

  if (index < 2) {
    orderKeyword.value = orderKeywordList[index];
    pageRequest.value.orderKeyword = orderKeyword.value.value;
  }
  else {

    orderType.value = orderTypeList[index - 2];
    pageRequest.value.orderType = orderType.value.value;
  }
  // 保存当前页面设置
  CURRENT_PAGE_SET();
  handleSelectAllFileInfo();
}

//3.修改图片大小模式
const handleCommandImageSize = (index: number) => {

  imageStyle.value = imageStyleSizeList[index];
  // 保存当前页面设置
  CURRENT_PAGE_SET();
}
//4.修改图片比例模式
const handleSacle = () => {
  if (scale.value.value == "original") {
    scale.value.value = "square";
    scale.value.label = "原始比例";
  } else {
    scale.value.value = "original";
    scale.value.label = "正方形";
  }
  // 保存当前页面设置
  CURRENT_PAGE_SET();
}


const filePages = ref<FilePage>()
const fileInfoList = ref<API.FileInfoList[]>([])
const moreFileInfoList = ref<API.FileInfoList[]>([])
//请求数据
const pageRequest = ref({
    current: 1,
    size: 100,
    orderKeyword: orderKeyword.value.value,
    orderType: orderType.value.value,
    imageTypeText: imageTypeText.value.value,
    albumId: -1,
    searchType: route.query.searchType as string,
    searchKeyword: route.query.searchKeyword as string,
})
const hasMore = ref(true);
const isLoading = ref(false)

// 监听事件
const showBus = () => {

    // 先清除之前的监听事件
    $bus.off('uploadSucess');
    $bus.on('uploadSucess', () => {
        handleSelectAllFileInfo();
    });

    //获取更多数据
    $bus.off('loadMoreData');

    $bus.on('loadMoreData', () => {
        if (hasMore.value && !isLoading.value)
            loadMoreData();
    });

    $bus.off('deletePictureSuceess');
    $bus.on('deletePictureSuceess', () => {
        handleSelectAllFileInfo();
    });
    $bus.off('removePictureSuceess');
    $bus.on('removePictureSuceess', () => {
        handleSelectAllFileInfo();
    });

}


// 获取文件列表
const handleSelectAllFileInfo = () => {
    isLoading.value = true
    pageRequest.value.current = 1
    selectFileListByKeyword(pageRequest.value).then(res => {
        if (res.code === 200) {
            filePages.value = res.data
            fileInfoList.value = res.data.records

        } else {
            ElMessage.error(res.message)
        }
    })
        .finally(() => {
            isLoading.value = false
        })
}
// 加载更多数据
const loadMoreData = async () => {
    isLoading.value = true
    pageRequest.value.current += 1
    try {
        const res = await selectFileListByKeyword(pageRequest.value)
        if (res.code === 200) {
            fileInfoList.value = [...fileInfoList.value, ...res.data.records]
            hasMore.value = pageRequest.value.current < res.data.pages
            moreFileInfoList.value = res.data.records
        }
    } finally {
        isLoading.value = false;
    }
}


</script>
<style scoped>
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

.search-keyword {
    display: flex;
    height: 50px;
    color: #1c1c1c;
    font-weight: 500;
    margin-left: 85px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 350px;
    font-size: 22px;
    align-items: center;
    user-select: none;
}
</style>