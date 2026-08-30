<template>
    <div id="picture" class="content">
        <!-- 顶部菜单 -->
        <div class="content-header">

            <div class="content-header-left">
                <div class="acontent-header-info">
                    <!-- 面包屑 -->
                    <el-breadcrumb :separator-icon="ArrowRight" class="album-header-breadcrumb">
                        <el-breadcrumb-item @click="router.push('/person')"
                            class="album-breadcrumb-item1">人物</el-breadcrumb-item>
                        <el-breadcrumb-item class="album-breadcrumb-item2">

                        </el-breadcrumb-item>
                    </el-breadcrumb>
                    <div class="content-header-info-item">
                        <div class="content-header-info-cover-shadow" @click="handleSelectAllPersonCover"></div>
                        <img class=" content-header-info-cover" :src="personAlbumInfo?.coverUrl" />

                    </div>
                    <div class="content-header-info-item">
                        <div class="picture-item-combination" v-if="showSetInput">
                            <el-input maxlength="10" type="text" :ref="(el) => { setPersonNameInput = el }"
                                @keyup.enter="handleUpdatePersonInfo" @blur="handleBlur"
                                style="width: 120px;border-right: 0;height: 25px;" size="small"
                                v-model="personInfoForm.personName" placeholder="添加名字">
                                <template #append>
                                    <el-button :icon="Select"
                                        :style="{ color: (personInfoForm.personName!=null)&&personInfoForm.personName.length > 0 ? '#3b75ff' : '' }"
                                        @click="handleUpdatePersonInfo" />
                                </template>
                            </el-input>
                        </div>

                        <div class="picture-item-combination picture-item-personName" v-else
                            @click="clickUpdatePersonName"> {{
                                personAlbumInfo?.personName == null ?
                                    "添加名字" :
                                    personAlbumInfo?.personName
                            }} </div>

                    </div>
                    <div class="content-header-info-item">

                        <el-select v-model="personInfoForm.personRelation" @change="handleUpdatePersonInfo" size="small"
                            placeholder="Select" style="width: 120px;color:  rgb(59, 117, 255);">
                            <el-option v-for="item in personRelationList" :key="item.value" :label="item.label"
                                :value="item.value" />
                            <template #prefix>
                                <i-material-symbols:account-circle-outline
                                    style="font-size: 16px;color: rgb(59, 117, 255);" />
                            </template>
                        </el-select>

                    </div>
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
            共{{ personAlbumInfo?.total }}张照片
        </div>
        <div class="pictureList">
            <!-- 图片列表 -->
            <PictureList parentComponent="PersonAlbum" :fileInfoList="fileInfoList" :moreFileInfoList="moreFileInfoList"
                :imageStyleSize="imageStyle.value" :scale="scale" :albumId="personAlbumInfo?.personId||(-1)">
            </PictureList>
        </div>
        <el-dialog :center="true" v-model="dialogVisible" title="更换人物封面" width="500" :append-to-body="true">


            <div class="dialog-cover-list">

                <div class="dialog-cover-item" v-for="(item, index) in personCoverUrls" :key="index"
                    @click="personInfoForm.faceId = item.faceId">
                    <div class="dialog-cover-item-cover-shadow"></div>

                    <img class="dialog-cover-item-img" :src="item.coverUrl"
                        :style="{ border: personInfoForm.faceId == item.faceId ? '3px solid #3b75ff' : '' }" />
                    <el-icon v-if="personInfoForm.faceId == item.faceId"
                        style="position: absolute;bottom: -20px;right: 32px;color: #3b75ff;">
                        <Top />
                    </el-icon>
                </div>
            </div>

            <template #footer>
                <div>
                    <el-button @click="dialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="handleUpdatePersonInfo">
                        确认
                    </el-button>
                </div>
            </template>
        </el-dialog>

    </div>
</template>





<script setup lang="ts">
import { Check, Select, ArrowRight } from '@element-plus/icons-vue'
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router'
import { selectPersonAlbumFileInfo, selectPersonById, selectAllPersonCover, updatePersonInfo } from '@/api/person/person'
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
interface personCover {
    faceId: number;
    coverUrl: string;
}
const personRelationList = [
    { label: '我自己', value: '我自己' },
    { label: '亲爱的', value: '亲爱的' },
    { label: '孩子', value: '孩子' },
    { label: '爸爸', value: '爸爸' },
    { label: '妈妈', value: '妈妈' },
    { label: '朋友', value: '朋友' },
    { label: '同事', value: '同事' },
    { label: '同学', value: '同学' },
    { label: '其他亲属', value: '其他亲属' },
    { label: '无关系', value: '无关系' }
];
const personAlbumInfo = ref<API.PersonAlbum>();

//路由const router = useRouter();
const route = useRoute();
const dialogVisible = ref(false);
const personId = ref(String(route.params.personId));
const showSetInput = ref(false);

const personInfoForm = ref({
    personName: personAlbumInfo.value?.personName || "",
    personRelation: personAlbumInfo.value?.personRelation,
    personId: personAlbumInfo.value?.personId,
    oldFaceId: personAlbumInfo.value?.faceId,
    faceId: personAlbumInfo.value?.faceId,//默认不更改face
})
const setPersonNameInput = ref();

const handleBlur = () => {
    setTimeout(() => {
        showSetInput.value = false;
    }, 200);
}
const clickUpdatePersonName = async () => {
    showSetInput.value = true;
    await nextTick();
    setPersonNameInput.value.focus();
}
const handleUpdatePersonInfo = async () => {

    const res = await updatePersonInfo(personInfoForm.value);
    if (res.code === 200) {
        ElMessage.success(res.message);
        handleSelectPersonById();
    }

    dialogVisible.value = false;

}


const personCoverUrls = ref<personCover[]>([]);
const handleSelectAllPersonCover = async () => {
    dialogVisible.value = true;
    const res = await selectAllPersonCover({
        personId: personId.value
    })
    if (res.code === 200) {
        personCoverUrls.value = res.data;
    }

}
onMounted(() => {
      //获取当前页面设置信息
   CURRENT_PAGE_GET();
    showBus();
    handleSelectAllFileInfo();
    handleSelectPersonById();
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
    size: 50,
    orderKeyword: orderKeyword.value.value,
    orderType: orderType.value.value,
    imageTypeText: imageTypeText.value.value,
    personId: personId.value,
})
const filePages = ref<FilePage>()
const fileInfoList = ref<API.FileInfoList[]>([])
const moreFileInfoList = ref<API.FileInfoList[]>([])

const hasMore = ref(true);
const isLoading = ref(false)


const handleSelectPersonById = async () => {
    const res = await selectPersonById({
        personId: personId.value
    })
    if (res.code === 200) {
        personAlbumInfo.value = res.data;
        personInfoForm.value.oldFaceId = res.data.faceId;
        personInfoForm.value.faceId = res.data.faceId;
        personInfoForm.value.personId = res.data.personId;
        personInfoForm.value.personName = res.data.personName;
        personInfoForm.value.personRelation = res.data.personRelation;
    }
}

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

    $bus.off('removePictureSuccess');
    $bus.on('removePictureSuccess', () => {
        handleSelectAllFileInfo();
    });
}


// 获取文件列表
const handleSelectAllFileInfo = () => {
    isLoading.value = true
    pageRequest.value.current = 1
    selectPersonAlbumFileInfo(pageRequest.value).then(res => {
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
        const res = await selectPersonAlbumFileInfo(pageRequest.value)
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


.acontent-header-info {
    width: 100%;
    height: 50px;
    display: flex;
    align-items: center;
    user-select: none;
    cursor: pointer;
}

.dialog-cover-list {
    width: 100%;
    height: 400px;
    overflow: auto;
    display: grid;
    grid-template-columns: repeat(auto-fill, 80px);
    gap: 20px;
    justify-content: center;
    align-content: start;
}

.dialog-cover-item {
    cursor: pointer;
    width: 80px;
    height: 80px;

    position: relative;
}

.dialog-cover-item-cover-shadow {
    width: 80px;
    height: 80px;
    position: absolute;
    border-radius: 50%;
    z-index: 2;
}

.dialog-cover-item-cover-shadow:hover {
    background: rgba(0, 0, 0, 0.3);
}

.dialog-cover-item-img {

    object-fit: cover;
    cursor: pointer;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    /* border:#3b75ff 3px solid; */
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



.content-header-info-cover {
    width: 36px;
    height: 36px;
    border-radius: 50%;
}

.content-header-info-cover-shadow {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    position: absolute;
    z-index: 2;
}

.content-header-info-cover-shadow:hover {
    background: rgba(0, 0, 0, 0.3);
}

.content-header-info-item {
    height: 60px;
    margin-left: 10px;
    display: flex;
    align-items: center;
}

.picture-item-combination {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 60px;
}

.picture-item-personName {
    font-size: 15px;
    white-space: nowrap;
}

.picture-item-personName:hover {
    color: #3b75ff;
}

:deep(.el-breadcrumb__inner) {
    color: #3b75ff;
    font-weight: 400;
}

:deep(.el-select__placeholder) {
    color: #3b75ff;
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
    margin-left: 25px;
    height: 20px;
    padding-top: 10px;
    padding-bottom: 10px;
    font-family: PingFangSC-Regular;
    font-size: 14px;
    color: rgb(153, 153, 153);
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