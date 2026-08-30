<template>
  <div id="picture" class="content">

    <!-- 顶部菜单 -->
    <div class="content-header">

      <div class="content-header-left">
        <h3 style="margin-left: 20px;   white-space: nowrap;">相似图片检测</h3>
        <div style="display: flex;align-items: center;">

          <span style="margin-left: 20px;   white-space: nowrap;">相似度：</span>
          <el-input-number style="width: 120px;" v-model="similarity" :precision="2" :step="0.01" :max="1" />
          <span style="margin-left: 20px;   white-space: nowrap;">检索数量：</span>
          <el-input-number style="width: 120px;" v-model="searchSize" :precision="0" :step="10" />
        </div>

        <el-button type="success" round class="content-header-left-button" @click="autoSelect" :disabled="isLoading">
          <span class="content-header-button-span">{{ '智能选择' }}</span>
        </el-button>
        <el-button type="primary" round class="content-header-left-button" @click="startCreateSimilar"
          :disabled="isLoading">
          <span class="content-header-button-span">开始检测</span>
        </el-button>

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
    <!-- 图片列表 -->
    <SimilarPictureList parentComponent="AllPicture" :fileInfoList="fileInfoList" :imageStyleSize="imageStyle.value"
      :scale="scale" :albumId="-1">
    </SimilarPictureList>

  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import $bus from '@/utils/bus.ts'
import { getSimilarFileList, createSimilarFileList } from '@/api/file/file'
import SimilarPictureList from '@/components/similar/SimilarPictureList.vue'
import { storage } from '@/utils/storage';

onMounted(() => {
   //获取当前页面设置信息
  CURRENT_PAGE_GET();
  showBus();
  handleSelectAllFileInfo();
});
onUnmounted(() => {

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
  storage.set<CurrentPageSet>('CURRENT_PAGE_SET3', {
    imageTypeText: imageTypeText.value,
    orderKeyword: orderKeyword.value,
    orderType: orderType.value,
    imageStyle: imageStyle.value,
    scale: scale.value,
  });
}
const CURRENT_PAGE_GET = () => {
  const currentPageSet = storage.get<CurrentPageSet>('CURRENT_PAGE_SET3');
  if (currentPageSet && currentPageSet.imageTypeText && currentPageSet.orderKeyword && currentPageSet.orderType && currentPageSet.imageStyle && currentPageSet.scale) {
    imageTypeText.value = currentPageSet.imageTypeText;
    orderKeyword.value = currentPageSet.orderKeyword;
    orderType.value = currentPageSet.orderType;
    imageStyle.value = currentPageSet.imageStyle;
    scale.value = currentPageSet.scale;
    pageRequest.value.imageTypeText = imageTypeText.value.value;
    
   
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


const fileInfoList = ref<API.FileInfoList[]>([])
const similarity = ref(0.60);
const searchSize = ref(30);
const isLoading = ref(false);
//请求数据
const pageRequest = ref({
  imageTypeText: imageTypeText.value.value,
})




// 监听事件
const showBus = () => {

  $bus.off('deletePictureSuceess');
  $bus.on('deletePictureSuceess', () => {
    handleSelectAllFileInfo();
  });
  $bus.off('removePictureSuceess');
  $bus.on('removePictureSuceess', () => {
    handleSelectAllFileInfo();
  });

}

const autoSelect = () => {

  $bus.emit('IsAutoSelect');

}
const startCreateSimilar = async () => {
  isLoading.value = true
  try {
    $bus.emit('IsLoading', true);
    const res = await createSimilarFileList({
      similarity: similarity.value,
      size: searchSize.value,
    });
    if (res.code === 200) {
      ElMessage.success(res.message)
      handleSelectAllFileInfo();

    } else {
      ElMessage.error(res.message)
    }
  }
  finally {
    isLoading.value = false
    $bus.emit('IsLoading', false);
  }
}
// 获取文件列表
const handleSelectAllFileInfo = async () => {

  const res = await getSimilarFileList(pageRequest.value);
  if (res.code === 200) {
    fileInfoList.value = res.data;
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
</style>