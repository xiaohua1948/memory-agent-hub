<template>
  <MediaViewer
    v-if="visible"
    :url-list="urlList"
    :file-id-list="fileIdList"
    :initial-index="initialIndex"
    :show-drawer="false"
    :z-index="99999"
    @close="close"
  />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import $bus from '@/utils/bus';
import MediaViewer from './MediaViewer.vue';

/**
 * 全局图片预览器
 * 任意位置通过 $bus.emit('openImageViewer', { urlList, initialIndex, fileIdList }) 打开全屏预览。
 * 用于非列表缩略图场景（相册封面、AI 助手消息照片等），统一全屏（覆盖 header）体验。
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

function open(payload: PreviewPayload) {
  if (!payload || !payload.urlList || payload.urlList.length === 0) return;
  urlList.value = payload.urlList;
  initialIndex.value = payload.initialIndex ?? 0;
  fileIdList.value = payload.fileIdList ?? [];
  visible.value = true;
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
