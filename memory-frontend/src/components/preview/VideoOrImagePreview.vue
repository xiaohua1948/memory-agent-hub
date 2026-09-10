<template>
  <div>
    <!-- 参数element-plus图片预览组件相似 -->
    <MediaViewer v-if="isShow" :url-list="previewSrcList" :fileId-list="fileIdList" :initialIndex="initialIndex"
      :zIndex="zIndex" :showDrawer="props.showDrawer" @close="closeViewer">
      <template #viewer>
        <slot name="viewer">
        </slot>
      </template>
    </MediaViewer>

    <div @click="openViewer" class="viewer">
      <el-image :class="{ 'picture-group-image': true, 'picture-group-image-animation': isImageChecked }" fit="cover"
        :src="src" loading="lazy" :preview-teleported="true" :show-progress="true" >
        <template #error>
          <div class="image-slot">
            <el-icon><i-ep-picture /></el-icon>
          </div>
        </template>
      </el-image>

      <div v-if="!isImage" class="video-tag">
        <i-ep:VideoPlay />
        <span>{{ videoTime }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import MediaViewerRaw from "./MediaViewer.vue";
// MediaViewer 是 JS 风格组件、未声明 slots 类型；本地断言为 any 以使用其模板内实际提供的 viewer 插槽
const MediaViewer = MediaViewerRaw as any;

import { ref } from "vue";

const props = defineProps({
  showDrawer: {
    type: Boolean,
    default: false,
  },
  fileIdList: {
    type: Array,
    default: [],
  },
  isImageChecked: {
    type: Boolean,
    default: false,
  },
  isImage: {
    type: Boolean,
    default: true,
  },
  videoTime: {
    type: String,
    default: "00:00",
  },
  src: {
    type: String,
    default: "",
  },
  previewSrcList: {
    type: Array,
    default: [],
  },
  initialIndex: {
    type: Number,
    default: 0,
  },
  width: {
    type: [Number, String],
    default: "",
  },
  height: {
    type: [Number, String],
    default: "",
  },
  zIndex: {
    type: Number,
    default: 999,
  },

});
const isShow = ref(false);
// 判断path是否为外链
const isExternal = (path: string) => {
  return /^(https?:|mailto:|tel:)/.test(path);
};

// 关闭预览
function closeViewer() {
  isShow.value = false;
}
//打开预览
function openViewer() {
  if (props.previewSrcList.length == 0) {
    return;
  }
  isShow.value = true;
}
</script>
<style scoped>
.viewer {
  position: relative;
  width: 100%;
  height: 100%;
}

/*图片css */
.picture-group-image {
  width: 100%;
  height: 100%;
  transition: transform 0.3s;
  transform: scale(1);
}

.picture-group-image-animation {
  transition: transform 0.3s;
  transform: scale(0.9);
}

/* 视频标识 */
.video-tag {
  position: absolute;
  display: flex;
  right: 5px;
  bottom: 5px;
  background-color: rgba(49, 49, 54, 0.76);
  border-radius: 8px;
  padding: 4px;
  color: #fff;
  color-scheme: light;
  font-size: 18px;
  align-items: center;
}

.video-tag span {
  font-size: 14px;
  margin-left: 3px;
}

/* 图片加载失败 */
.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 30px;
}
</style>