<template>
  <div id="picture" class="content">
    <!-- 顶部菜单 -->
    <div class="content-header">

      <div class="content-header-left">
        <el-dropdown trigger="click" @command="handleCommandUpload">
          <el-button type="primary" round class="content-header-left-button">
            <i-ep-upload />
            <span class="content-header-button-span">上传相册</span>
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
        <el-button type="warning" round class="content-header-left-button" @click="updateFormVisible = true">
          <i-ep-setting />
          <span class="content-header-button-span">修改相册</span>
        </el-button>
        <el-button @click="clickDownloadAlbum" type="success" round class="content-header-left-button">
          <i-ci:cloud-download />
          <span class="content-header-button-span">下载相册</span>
        </el-button><el-button @click="deleteFormVisible = true" type="danger" round class="content-header-left-button">
          <i-solar-album-broken />
          <span class="content-header-button-span">删除相册</span>
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
        <!-- 排序 -->
        <el-dropdown trigger="click" @command="handleCommandOrderType">
          <el-button text>
            <i-ep-calendar />
            <span class="content-header-button-span">排序时间</span>
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
      <div class="album-header-info-name">
        <!-- 面包屑 -->

        <el-breadcrumb :separator-icon="ArrowRight" class="album-header-breadcrumb">
          <el-breadcrumb-item :to="{ path: '/album' }" class="album-breadcrumb-item1">全部相册</el-breadcrumb-item>
          <el-breadcrumb-item class="album-breadcrumb-item2">{{ albumInfo.albumName }}</el-breadcrumb-item>
        </el-breadcrumb>

        <div class="album-header-info-text">
          <span>共{{ albumInfo.imageCount }}张图片，</span>
          <span>{{ albumInfo.videoCount }}个视频，</span>
          <span>创建于{{ formatDate(albumInfo.createTime) }}</span>
        </div>
      </div>
      <div class="album-header-info-des">
        <span>公告：{{ albumInfo.description || '相册公告' }}</span>

      </div>
    </div>
    <div class="pictureList">
      <!-- 图片列表 -->
      <PictureList parentComponent="AlbumPicture" :fileInfoList="fileInfoList" :moreFileInfoList="moreFileInfoList"
        :imageStyleSize="imageStyle.value" :scale="scale" :albumId="albumInfo.albumId">
      </PictureList>
    </div>

    <el-dialog draggable :modal="false" v-model="updateFormVisible" title="修改相册" width="400" center
      @closed="reSetalbumInfoForm">

      <el-form :model="albumInfoForm" label-position="left">
        <el-form-item label="名称">
          <el-input v-model="albumInfoForm.albumName" maxlength="30" placeholder="请输入新相册名称" show-word-limit />
        </el-form-item>
        <el-form-item label="公告">
          <el-input v-model="albumInfoForm.description" maxlength="80" placeholder="请输入新相册公告" show-word-limit
            type="textarea" />
        </el-form-item>
        <el-form-item>
          <div style="display: flex;justify-content: center;width: 100%;">
            <el-button type="primary" @click="onUpdateFormSubmit">确认</el-button>
            <el-button @click="updateFormVisible = false">取消</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>

    <el-dialog draggable :modal="false" v-model="deleteFormVisible" title="确认删除此相册吗？" width="400" center>
      <div style="display: flex;align-items: center;justify-content: center;">
        <el-icon style="color: #F56C6C;font-size: large;vertical-align: middle;margin-right:5px">
          <WarningFilled />
        </el-icon>
        <span>相册一经删除将无法恢复，请谨慎操作</span>
      </div>
      <div style="display: flex;align-items: center;justify-content: center;margin-top: 20px;">
        <el-checkbox v-model="deleteForm.isDeletePicture" style="margin-right: 5px;" />同时将该相册内的所有图片加入回收站
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="danger" @click="onDeleteFormSubmit">确认</el-button>
          <el-button @click="deleteFormVisible = false">取消</el-button>
        </div>
      </template>

    </el-dialog>


  </div>
</template>





<script setup lang="ts">
import { Check, ArrowRight } from '@element-plus/icons-vue'
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import $bus from '@/utils/bus.ts'
import PictureList from '@/components/picture/PictureList.vue'
import { useRoute, useRouter } from 'vue-router'
import { selectAlbumById, updateAlbumInfo, getDownloadAlbumToken, deleteAlbum } from '@/api/album/album'
import { selectAllFileInfo } from '@/api/file/file'
import requestPublicConfig from "@/api/config";
import { storage } from '@/utils/storage';
interface FilePage {
  records: API.FileInfoList;
  total: number;
  size: number;
  current: number;
  pages: number;
}



//路由
const route = useRoute();
const router = useRouter();
const albumInfo = ref<API.albumInfo>(
  {
    //默认是路由传来的
    albumId: Number(route.params.albumId),
    userId: 100000001,
    albumName: '',
    coverUrl: '',
    createTime: '',
    updateTime: '',
    imageCount: 0,
    videoCount: 0,
    description: '',
  }
);
const albumInfoForm = reactive({
  albumId: 100000001,
  albumName: '',
  description: '',
})
const reSetalbumInfoForm = () => {
  albumInfoForm.albumId = albumInfo.value.albumId;
  albumInfoForm.albumName = albumInfo.value.albumName || '';
  albumInfoForm.description = albumInfo.value.description || '';
}
const clickDownloadAlbum = async () => {
  ElMessage.info("下载开始...");

  // 获取带鉴权的临时下载令牌
  const res = await getDownloadAlbumToken({
    albumId: albumInfo.value.albumId,
  })
  if (res.code !== 200) {
    ElMessage.error("下载失败！");
    return;
  }
  else {
    const token = res.data;
    // 构造下载链接
    const downloadUrl = requestPublicConfig.baseUrl + `/album/downloadAlbumByToken?downloadToken=${encodeURIComponent(token)}`;

    // 创建隐藏iframe触发原生下载
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.src = downloadUrl;
    iframe.onload = () => {
      document.body.removeChild(iframe);

    };
    document.body.appendChild(iframe);

  }

}
//修改相册信息
const updateFormVisible = ref(false);
const onUpdateFormSubmit = async () => {
  const res = await updateAlbumInfo(albumInfoForm);
  if (res.code == 200) {
    updateFormVisible.value = false;
    ElMessage.success('修改成功');
    //更新相册信息
    handleSelectAlbumById();
  }
  else {
    ElMessage.error('修改失败');
  }
}
//删除相册
const deleteFormVisible = ref(false);
const deleteForm = reactive({
  albumIds: [0],
  isDeletePicture: false
})
const onDeleteFormSubmit = async () => {
  deleteForm.albumIds = [albumInfo.value.albumId];
  deleteFormVisible.value = false;
  const res = await deleteAlbum(deleteForm);
  if (res.code == 200) {
    ElMessage.success('删除成功');
    router.push('/album');
  }
  else {
    ElMessage.error('删除失败');
  }

}
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
onMounted(() => {
  //获取当前页面设置信息
  CURRENT_PAGE_GET();
  showBus();
  //查询相册信息
  handleSelectAlbumById();
  //查询照片信息
  handleSelectAllFileInfo();
});
onUnmounted(() => {
  $bus.off('uploadSucess');
  $bus.off('loadMoreData');
  $bus.off('deletePictureSuceess');
  $bus.off('removePictureSuceess');
})
const handleSelectAlbumById = async () => {
  const res = await selectAlbumById({
    albumId: albumInfo.value.albumId,
  });
  if (res.code == 200) {
    if (res.data) {
      albumInfo.value = res.data;
      reSetalbumInfoForm();
    }
  }
}

/*选中菜单信息*/

// 处理按钮点击事件:组件通信,传去上传方式
const handleCommandUpload = (value: string) => {

  const uploadParams = {
    uploadType: value,
    albumId: route.params.albumId,//上传到指定相册id
  }

  $bus.emit('showUploadList', uploadParams);
};





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
  albumId: albumInfo.value.albumId
})
const hasMore = ref(true);
const isLoading = ref(false)




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

/******************************​ 工具函数 ​******************************/
// 日期格式化
const formatDate = (isoString: string) => {
  const date = new Date(isoString)

  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 `
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



/* 相册信息 */
.album-header-info {
  width: 100%;
  height: 80px;
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
  font-size: 20px;
  color: rgb(59, 117, 255);
  white-space: nowrap;
}

.album-breadcrumb-item2 {
  user-select: none;
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
  height: calc(100% - 80px);
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