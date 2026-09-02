<template>
  <div id="picture" class="content">

    <!-- 顶部菜单 -->
    <div class="content-header">

      <div class="content-header-left">
        <el-dropdown trigger="click" @command="handleCommandUpload">
          <el-button type="primary" round class="content-header-left-button">
            <i-ep-upload />
            <span class="content-header-button-span">上传</span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu class="content-header-menu">
              <el-dropdown-item class="content-header-left-item" command="uploadImage"> <i-ep:picture
                  style="font-size: 20px;margin-right: 10px;" />上传照片</el-dropdown-item>
              <el-dropdown-item class="content-header-left-item" command="uploadFolder"><i-ep:folder
                  style="font-size: 20px;margin-right: 10px;" />上传文件夹</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

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
    <SelectPictureList parentComponent="AllPicture" :fileInfoList="fileInfoList" :moreFileInfoList="moreFileInfoList"
        :imageStyleSize="imageStyleSize" :scale="scale" :albumId="-1" :emit-event="emitEvent">
    </SelectPictureList>

  </div>
</template>

<script setup lang="ts">
import { Check } from '@element-plus/icons-vue'
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import $bus from '@/utils/bus.ts'
import { selectAllFileInfo } from '@/api/file/file'
import SelectPictureList from '@/components/select_picture/SelectPictureList.vue'

const props = withDefaults(defineProps<{ emitEvent?: string }>(), {
    emitEvent: 'addSelectPicture',
});

interface FilePage {
  records: API.FileInfoList;
  total: number;
  size: number;
  current: number;
  pages: number;
}

/*选中菜单信息*/
// 处理按钮点击事件:组件通信,传去上传方式
const handleCommandUpload = (value: string) => {
  const uploadParams = {
    uploadType: value,
    albumId: -1,//-1表示不上传到相册
  }
  $bus.emit('showUploadList', uploadParams);
};

onMounted(() => {
  showBus();
  handleSelectAllFileInfo();
});
onUnmounted(() => {
  $bus.off('uploadSucess');
  $bus.off('loadMoreData');
  $bus.off('deletePictureSuceess');
  $bus.off('removePictureSuceess');
})

const imageTypeList = [
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
const imageTypeText = ref(imageTypeList[0]);
const handleCommandImageType = (index: number) => {
  imageTypeText.value = imageTypeList[index];
  pageRequest.value.imageTypeText = imageTypeText.value.value;
  handleSelectAllFileInfo();
}

// 排序方式
const orderTypeList = [
  {
    value: 'asc',
    label: '升序排列',
  },
  {
    value: 'desc',
    label: '降序排列',
  },
]

// 排序关键词
const orderKeywordList = [
  {
    value: 'upload_time',
    label: '上传时间',
  },
  {
    value: 'date_time_original',
    label: '拍摄时间',
  },
]
// 默认方式
const orderKeyword = ref(orderKeywordList[1]);
const orderType = ref(orderTypeList[1]);
const handleCommandOrderType = (index: number) => {

  if (index < 2) {
    orderKeyword.value = orderKeywordList[index];
    pageRequest.value.orderKeyword = orderKeyword.value.value;
  }
  else {

    orderType.value = orderTypeList[index - 2];
    pageRequest.value.orderType = orderType.value.value;
  }
  handleSelectAllFileInfo();
}
const scale = reactive({
  label: "原始比例",
  value: "square"
});
const handleSacle = () => {
  if (scale.value == "original") {
    scale.value = "square";
    scale.label = "原始比例";
  } else {
    scale.value = "original";
    scale.label = "正方形";
  }
}

/*图片样式*/
const imageStyleSizeList = [
  {
    label: '大图模式',
    value: 150,
  },
  {
    label: '中图模式',
    value: 120,
  },
  {
    label: '小图模式',
    value: 90,
  },
]
const imageStyleSize = ref(imageStyleSizeList[1].value);
const imageStyleText = ref(imageStyleSizeList[1].label);
const handleCommandImageSize = (index: number) => {

  imageStyleSize.value = imageStyleSizeList[index].value;
  imageStyleText.value = imageStyleSizeList[index].label;
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
  albumId: -1
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
  $bus.off('removePictureSuccess');
  $bus.on('removePictureSuccess', () => {
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
