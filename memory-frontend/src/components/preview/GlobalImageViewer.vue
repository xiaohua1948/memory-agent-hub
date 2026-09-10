<template>
  <MediaViewer
    v-if="visible"
    :url-list="urlList"
    :file-id-list="fileIdList"
    :initial-index="initialIndex"
    :show-drawer="showDrawer"
    :z-index="1800"
    @close="close"
    @switch="handleSwitch"
  >
    <!-- 携带照片 id 时提供与首页一致的工具栏：下载 / 添加相册 / 照片详情 / 照片类型标签 / 删除 -->
    <template #viewer>
      <div v-if="hasFileIds" class="my-image-viewer-action">
        <div class="my-image-viewer-action-inner">
          <el-tooltip content="下载图片" placement="top" :show-after="300">
            <i-ep:download class="my-image-viewer-icon" @click="downloadCurrent" />
          </el-tooltip>
          <el-tooltip content="添加相册" placement="top" :show-after="300">
            <i-solar:album-bold-duotone class="my-image-viewer-icon" @click="() => { showAddAlbum = true; }" />
          </el-tooltip>
          <el-tooltip content="照片详情" placement="top" :show-after="300">
            <i-ep:Warning class="my-image-viewer-icon" @click="() => { showDrawer = !showDrawer; }" />
          </el-tooltip>
          <el-tooltip content="照片类型标签" placement="top" :show-after="300">
            <i-tabler:tag-plus class="my-image-viewer-icon" @click="openTag" />
          </el-tooltip>
          <el-tooltip content="删除照片" placement="top" :show-after="300">
            <i-material-symbols:delete-outline class="my-image-viewer-icon" @click="deleteCurrent" />
          </el-tooltip>
        </div>
      </div>
    </template>
  </MediaViewer>

  <!-- 添加相册（选中列表为空时自动针对当前预览的照片） -->
  <AddAlbum v-model:show="showAddAlbum" :checked-images="[[]]" :currentFileId="currentFileId" />

  <!-- 照片类型标签（针对当前预览照片；checkedImages 必传，传空数组表示无多选） -->
  <PictureTag v-model:show="showTag" :checked-images="[[]]" :currentFileId="currentFileId" />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import $bus from '@/utils/bus';
import requestPublicConfig from '@/api/config';
import MediaViewerRaw from './MediaViewer.vue';
// MediaViewer 是 JS 风格组件、未声明 slots 类型；本地断言为 any 以使用其模板内实际提供的 viewer 插槽
const MediaViewer = MediaViewerRaw as any;
import AddAlbum from '@/components/album/AddAlbum.vue';
import PictureTag from '@/components/picture/PictureTag.vue';
import { getDownloadToken, deleteFileByIds } from '@/api/file/file';

/**
 * 全局图片预览器
 * 任意位置通过 $bus.emit('openImageViewer', { urlList, initialIndex, fileIdList }) 打开全屏预览。
 * 用于非列表缩略图场景（相册封面、AI 助手消息照片等），统一全屏（覆盖 header）体验。
 * 携带 fileIdList 时（如 Agent 查询返回的照片），可下载、添加相册、查看照片详情（媒体数据）。
 */
interface PreviewPayload {
  urlList: string[];
  initialIndex?: number;
  fileIdList?: string[];
}

const visible = ref(false);
const urlList = ref<string[]>([]);
const fileIdList = ref<string[]>([]);
const initialIndex = ref(0);
// 照片详情（媒体数据）抽屉
const showDrawer = ref(false);
// 添加相册弹窗
const showAddAlbum = ref(false);
// 当前预览照片的文件 id（下载/添加相册均针对当前照片）
const currentFileId = ref('');
// 标签修改弹窗显隐
const showTag = ref(false);

const hasFileIds = computed(() => fileIdList.value.length > 0);

function open(payload: PreviewPayload) {
  if (!payload || !payload.urlList || payload.urlList.length === 0) return;
  urlList.value = payload.urlList;
  initialIndex.value = payload.initialIndex ?? 0;
  fileIdList.value = payload.fileIdList ?? [];
  showDrawer.value = false;
  showAddAlbum.value = false;
  currentFileId.value = fileIdList.value[initialIndex.value] ?? '';
  visible.value = true;
}

// 预览器内切换照片时，同步当前文件 id（供下载/添加相册使用）
function handleSwitch(index: number) {
  currentFileId.value = fileIdList.value[index] ?? '';
}

// 下载当前预览的照片（与首页一致：临时下载令牌 + iframe 触发下载）
async function downloadCurrent() {
  if (!currentFileId.value) return;
  try {
    const res = await getDownloadToken({ fileIds: [currentFileId.value] });
    if (res.code !== 200 || !res.data) {
      ElMessage.error('下载失败！');
      return;
    }
    const downloadUrl =
      requestPublicConfig.baseUrl + `/file/downloadFileByToken?downloadToken=${encodeURIComponent(res.data)}`;
    const iframe = document.createElement('iframe');
    iframe.style.display = 'none';
    iframe.src = downloadUrl;
    iframe.onload = () => {
      document.body.removeChild(iframe);
      ElMessage.success('下载开始');
    };
    document.body.appendChild(iframe);
  } catch {
    ElMessage.error('下载失败');
  }
}

// 打开标签修改弹窗（针对当前预览照片）
function openTag() {
  if (!currentFileId.value) return;
  showTag.value = true;
}

// 删除当前预览照片（危险操作，二次确认）
async function deleteCurrent() {
  if (!currentFileId.value) return;
  try {
    await ElMessageBox.confirm(
      '确定删除这张照片吗？删除后不可恢复。',
      '删除照片',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    );
  } catch {
    return; // 用户取消
  }
  try {
    const res = await deleteFileByIds({ fileIds: [currentFileId.value] });
    if (res.code === 200) {
      ElMessage.success('删除成功');
      close();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败');
  }
}

function close() {
  visible.value = false;
}

onMounted(() => {
  $bus.on('openImageViewer', (payload: any) => open(payload as PreviewPayload));
});

onUnmounted(() => {
  $bus.off('openImageViewer');
});
</script>

<style scoped>
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
