<template>
  <div id="picture" class="content">
    <!-- 顶部菜单 -->
    <div class="content-header">

      <div class="content-header-left">
        <div class="album-header-info-name">
        <!-- 面包屑 -->
        <el-breadcrumb :separator-icon="ArrowRight" class="album-header-breadcrumb">
          <el-breadcrumb-item @click="router.back()" class="album-breadcrumb-item1">地点</el-breadcrumb-item>
          <el-breadcrumb-item class="album-breadcrumb-item2">{{ locationValue }}</el-breadcrumb-item>
        </el-breadcrumb>

      </div>
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
                :style="{ color: orderType.value == orderTypeList[0].value ? '#409EFF' : '' }" :command="2">
                <span>{{ orderTypeList[0].label }}</span>
              </el-dropdown-item>
              <el-dropdown-item icon="Bottom" class="content-header-right-item"
                :style="{ color: orderType.value == orderTypeList[1].value ? '#409EFF' : '' }" :command="3">
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
                :style="{ color: imageStyle.label == item.label ? '#409EFF' : '' }"
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


    <div class="album-header-info">
            共{{ filePages?.total}}张照片
        </div>
    <div class="pictureList">
    <!-- 图片列表 -->
    <PictureList  parentComponent="LocationAlbum" :fileInfoList="fileInfoList" :moreFileInfoList="moreFileInfoList"
      :imageStyleSize="imageStyle.value" :scale="scale" :albumId="-1">
    </PictureList>
    </div>


  </div>
</template>





<script setup lang="ts">
import { Check, ArrowRight } from '@element-plus/icons-vue'
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import { selectAllFileInfo } from '@/api/file/file'
import $bus from '@/utils/bus.ts'
import router from '@/router';
import { storage } from '@/utils/storage';
interface FilePage {
  records: API.FileInfoList;
  total: number;
  size: number;
  current: number;
  pages: number;
}


//路由const router = useRouter();
const route = useRoute();

const locationLevel = ref(String(route.params.locationLevel));
const locationValue = ref(String(route.params.locationValue));


onMounted(() => {
   //获取当前页面设置信息
   CURRENT_PAGE_GET();
  showBus();
  handleSelectAllFileInfo();
});
onUnmounted(() => {
  $bus.off('loadMoreData');
  $bus.off('deletePictureSuceess');
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




//请求数据
const pageRequest = ref({
  current: 1,
  size: 10,
  orderKeyword: orderKeyword.value.value,
  orderType: orderType.value.value,
  imageTypeText: imageTypeText.value.value,
  albumId: -1,
  locationLevel: locationLevel.value,
  locationValue: locationValue.value
})
const filePages = ref<FilePage>()
const fileInfoList = ref<API.FileInfoList[]>([])
const moreFileInfoList = ref<API.FileInfoList[]>([])

const hasMore = ref(true);
const isLoading = ref(false)

// 监听事件
const showBus = () => {
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
}


// 获取文件列表
const handleSelectAllFileInfo = () => {
  isLoading.value = true
  pageRequest.value.current = 1
  selectAllFileInfo(pageRequest.value).then(res => {
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
    const res = await selectAllFileInfo(pageRequest.value)
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

/* 顶部菜单 */
.content-header {
  z-index: 4;
  width: 100%;
  height: 60px;
  display: flex;
}

/* 左边按钮 */
.content-header-left-button {
  width: 100px;
  margin-left: 10px;
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



.album-header-info {
    width: 100%;
    margin-left: 25px;
    height: 20px;
    padding-top: 10px;
    padding-bottom: 10px;
    font-family: PingFangSC-Regular;
    font-size: 14px;
    color: rgb(153, 153, 153);
}


.album-header-info-name {
  width: 100%;
  height: 50px;
  display: flex;
}

/* 面包屑 */
.album-header-breadcrumb {
  height: 100%;
  margin-left: 20px;
  margin-right: auto;
  display: flex;
  flex-direction: row;
}

.album-breadcrumb-item1 {
  user-select: none;
  cursor: pointer;
  font-size: 20px;
  color: rgb(59, 117, 255);
  white-space: nowrap;
}

.album-breadcrumb-item2 {
  user-select: none;
  cursor: default;
  font-size: 18px;
  color: #333;
  white-space: nowrap;
}

.album-header-info-text {
  display: flex;
  flex-direction: row;
  align-items: center;
  margin-right: 20px;
  font-family: SFUIText, PingFangSC-Regular, Helvetica Neue, Helvetica, Arial, sans-serif;
  user-select: none;
  font-size: 14px;
  color: #999;
}

.album-header-info-text span {
  margin-right: 1px;

  white-space: nowrap;
}

.album-header-info-des {
  color: #333;
  font-size: 15px;
  height: 30px;
  display: flex;
  align-items: center;
  margin-left: 20px;
  vertical-align: middle;
  cursor: pointer;
  white-space: nowrap;
}

:deep(.el-breadcrumb__inner) {
  color: rgb(59, 117, 255);
  font-weight: 400;
}

.pictureList {
    width: 100%;
    height: calc(100% - 40px);
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
</style>