<template>
    <div id="sharePage">
        <GlobalHeader :showSearh="false" />

        <div class="sharePage-main">
            <div class="sharePage-sider">
                <div class="sharePage-user">
                    <img class="sharePage-user-avatar" :src="shareInfo.sharePersonAvatar" >
                </img>
                    <span class="sharePage-user-name">来自{{ shareInfo.sharePersonName }}的分享</span>
                </div>
                <div class="share-image">
                    <div class="image-container" :class="`count-${Math.min(shareImageUrlList.length, 3)}`">
                        <el-image v-for="(item, index) in shareImageUrlList" :key="index" :src="item" alt=""
                            fit="cover" />
                        <div class="image-count">共{{ fileIds.length }}张图片</div>
                    </div>
                </div>

                <div class="sharePage-title">
                    <div class="sharePage-name">照片合集</div>
                    <div class="sharePage-failure-time">
                        {{ formatDate(shareInfo.expireTime) }}失效
                    </div>
                </div>
                <div class="sharePage-button">
                    <el-button style="width: 130px;height:36px;border-radius: 20px;" color="#3174ff" type="primary"
                        @click="clickSaveShare">保存全部</el-button>

                </div>
                <div class="sharePage-button" style="margin-top: 10px;">

                    <el-button link type="primary" @click="copyShareUrl">复制链接</el-button>
                </div>
                <div class="sharePage-desc">
                    <div class="sharePage-desc-item">
                        <p>图片</p>
                        <p>{{ shareInfo.imageCount }}</p>
                    </div>
                    <div class="sharePage-desc-item">
                        <p>视频</p>
                        <p>{{ shareInfo.videoCount }}</p>
                    </div>
                    <div class="sharePage-desc-item">
                        <p>浏览次数</p>
                        <p>{{ shareInfo.visitCount }}</p>
                    </div>
                </div>

            </div>
            <div class="sharePage-content">
                <!-- 顶部菜单 -->
                <div class="content-header">
                    <div class="content-header-left">
                    </div>
                    <div class="content-header-right">


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

                <!-- 图片列表 -->
                <SharePictureList parentComponent="SharePage" :fileInfoList="fileInfoList" :moreFileInfoList="[]"
                    :imageStyleSize="imageStyleSize" :scale="scale" :albumId="-1" :shareToken="shareToken">
                </SharePictureList>


            </div>
        </div>
        <el-dialog v-model="loginForm" style="max-width: 480px;">
            <LoginForm :showfoot="false" />
        </el-dialog>

        <el-dialog v-model="registerForm" style="max-width: 480px;">
            <RegisterForm :showfoot="false" />
        </el-dialog>
    </div>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router';
import GlobalHeader from '@/layouts/GlobalHeader.vue'
import { Check } from '@element-plus/icons-vue'
import { onMounted, reactive, ref } from 'vue';
import { getShareFileInfo, saveSharePicture } from '@/api/file/file'
import SharePictureList from '@/components/share/SharePictureList.vue'
import LoginForm from '@/components/auth/LoginForm.vue';
import RegisterForm from '@/components/auth/RegisterForm.vue';
import $bus from '@/utils/bus.ts';
const router = useRouter();
const route = useRoute();
const shareToken = ref(route.params.shareToken);
const shareUrl = ref(`${window.location.origin}/share/${shareToken.value}`);


const shareInfo = ref({
    sharePersonName: '',
    sharePersonAvatar: '',
    visitCount: 0,
    imageCount: 0,
    videoCount: 0,
    expireTime: '',
}
)
const fileIds = ref<string[]>([]);
const loginForm = ref(false);
const registerForm = ref(false);
const clickSaveShare = async () => {
    if (!checkLogin()) return;

    const res = await saveSharePicture({
        fileIds: fileIds.value,
        shareToken: String(shareToken.value),
    });
    if (res.code === 200) {
        ElMessage.success('保存成功');
    }
}
const checkLogin = () => {
    let token = localStorage.getItem('token');
    if (!token) {
        ElMessage.warning('请先登录');
        return false;
    }
    return true
}
// 复制操作
const copyShareUrl = async () => {
    if (!checkLogin()) return;
    try {
        // 现代浏览器方案（推荐）
        if (navigator.clipboard) {
            await navigator.clipboard.writeText(shareUrl.value);
            ElMessage.success('链接已复制到剪切板');
            return;
        }
        // 兼容旧版浏览器的降级方案
        const textarea = document.createElement('textarea');
        textarea.value = shareUrl.value;
        textarea.style.position = 'fixed';  // 防止滚动
        document.body.appendChild(textarea);
        textarea.select();

        const result = document.execCommand('copy');
        document.body.removeChild(textarea);

        if (result) {
            ElMessage.success('链接已复制');
        } else {
            throw new Error('复制失败');
        }
    } catch (err) {
    }
};
const showBus = () => {
    $bus.off('loginForm');
    $bus.on('loginForm', (val) => {
        loginForm.value = <boolean>val;
        if (!val) {
            $bus.emit('login');
        }
    })
    $bus.off('registerForm');
    $bus.on('registerForm', (val) => {

        registerForm.value = <boolean>val;
    })
}
onMounted(() => {
    showBus();
    selectShareFileInfo();
});
const selectShareFileInfo = () => {
    getShareFileInfo({
        shareToken: shareToken.value,
    }).then(res => {
        if (res.code === 200) {
            shareInfo.value = res.data
            fileInfoList.value = res.data.fileInfoList;
            initShareImageUrlList();
        } else {
            ElMessage.error(res.message)
        }
    })
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
const imageStyleSize = ref(imageStyleSizeList[1].value);
const imageStyleText = ref(imageStyleSizeList[1].label);
const handleCommandImageSize = (index: number) => {
    imageStyleSize.value = imageStyleSizeList[index].value;
    imageStyleText.value = imageStyleSizeList[index].label;
}

const fileInfoList = ref<API.FileInfoList[]>([])

const shareImageUrlList = ref(['']);
const initShareImageUrlList = () => {
    shareImageUrlList.value = [];
    let count1 = 0;
    let count2 = 0;
    let count3 = 0;
    fileInfoList.value.forEach((fileInfo) => {
        fileInfo.fileList.find((file) => {
            if (count1 < 3) {
                shareImageUrlList.value.push(file.thumbnailUrl);
            }
            fileIds.value.push(file.fileId);
            count1++;

            if (file.category == "image") {
                count2++;
            }
            if (file.category == "video") {
                count3++;
            }
        })
    })
    shareInfo.value.imageCount = count2;
    shareInfo.value.videoCount = count3;
}


const isLoading = ref(false)

const formatDate = (isoString: string) => {
    const date = new Date(isoString);

    // 年月日补零
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // 03[6,8](@ref)
    const day = String(date.getDate()).padStart(2, '0');        // 04[6,8](@ref)

    // 时间补零与时段判断
    const hours = date.getHours();
    const minutes = String(date.getMinutes()).padStart(2, '0'); // 02[6](@ref)
    const period = hours >= 12 ? '下午' : '上午';                // 时段[5](@ref)

    // 返回两种可选格式：
    // 方案1：24小时制带时段（输出：2025年03月04日 下午14:02）
    return `${year}年${month}月${day}日${String(hours).padStart(2, '0')}:${minutes}`;

    // 方案2：12小时制带时段（输出：2025年03月04日 下午02:02）
    //const formattedHours = String(hours % 12 || 12).padStart(2, '0'); // 02[6,8](@ref)
    //return `${year}年${month}月${day}日${formattedHours}:${minutes}`;
};


</script>
<style scoped>
#sharePage {
    width: 100%;
    height: 100%;
    position: relative;
}

.sharePage-main {
    width: 100%;
    height: calc(100% - 60px);
    position: fixed;
    display: flex;
    top: 60px
}

.sharePage-sider {
    user-select: none;
    height: 100%;
    width: 210px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
    box-shadow: 0 3px 10px 0 rgba(0, 0, 0, .06);
}

.sharePage-user {
    display: flex;
    height: 100px;
    align-items: center;
    justify-content: center;
    gap: 5px;
    border-bottom: 1px solid #e5e5e5;
}

.sharePage-user-name {
    font-size: 13px;
    color: #333;
    height: 17px;
    line-height: 17px;
}

.sharePage-user-avatar {
    width: 50px;
    height: 50px;
    transition: transform 0.2s ease;
    border-radius: 50%;
    object-fit: cover;
}

/* 基础容器 */
.share-image {
    margin-top: 20px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
}

.image-container {
    position: relative;
    width: 140px;
    /* 修改后 */
    height: 140px;
    /* 修改后 */
    display: grid;
    gap: 5px;
    overflow: hidden;
    border-radius: 10px;
    box-shadow: 6px 2px 12px rgba(0, 0, 0, 0.15);
    /* 更强的向右下阴影 */
}

/* 2张图片布局调整 */
.image-container.count-2 {
    grid-template: 1fr / 81px 55px;
    /* 原93px→81px(140/160 * 93) 63px→55px */
}

/* 3张图片布局调整 */
.image-container.count-3 {
    grid-template:
        "main sub1" 70px
        /* 原80px→70px(140/160 * 80) */
        "main sub2" 70px
        / 81px 55px;
    /* 同步count-2的列宽 */
}

/* 其他样式保持不变 */
.image-container.count-1 {
    grid-template: 1fr / 1fr;
}

.image-container.count-3 .el-image:nth-child(1) {
    grid-area: main;
}

.image-container.count-3 .el-image:nth-child(2) {
    grid-area: sub1;
}

.image-container.count-3 .el-image:nth-child(3) {
    grid-area: sub2;
}

.el-image {
    width: 100%;
    height: 100%;
    background: #f5f5f5;
}

.image-count {
    background: rgba(0, 0, 0, .6);
    height: 20px;
    border-radius: 13.5px;
    line-height: 20px;
    text-align: center;
    color: #fff;
    font-weight: 400;
    font-size: 14px;
    position: absolute;
    right: 8px;
    bottom: 10px;
    padding: 0 10px;
}

.sharePage-title {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-top: 20px;
}

.sharePage-name {

    font-family: PingFangSC-Semibold;
    font-size: 16px;
    height: 22px;
    line-height: 22px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    color: #333;
    max-width: 180px;
}

.sharePage-failure-time {
    margin-top: 6px;
    height: 17px;
    line-height: 17px;
    font-size: 13px;
    color: #de3333;
}

.sharePage-button {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 20px;
}

.sharePage-desc {
    width: 150px;
}

.sharePage-desc-item {
    height: 25px;
    line-height: 25px;
    font-size: 12px;
    color: #666;
    display: flex;
    justify-content: space-between;
}










/* 照片内容 */
.sharePage-content {
    width: calc(100% - 210px);
    height: 100%;
    position: relative;
}

.content-header {
    z-index: 4;
    width: 100%;
    height: 60px;
    display: flex;
    box-shadow: 0 2px 6px 0 rgba(0, 0, 0, .06);
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