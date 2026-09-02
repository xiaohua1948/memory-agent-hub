<template>
  <div id="basicLayout">
    <GlobalHeader :showSearh="true" />
    <AgentChat />
    <div class="GlobalBody">
      <GlobalSider />
      <div class="GlobalContent">
        <router-view></router-view>
        <div class="GlobalUpload">
          <FileUpload />
        </div>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import GlobalHeader from '@/layouts/GlobalHeader.vue'
import GlobalSider from "@/layouts/GlobalSider.vue";
import FileUpload from '@/components/file/FileUpload.vue'
import AgentChat from '@/components/ai/AgentChat.vue'
</script>

<style scoped>
#basicLayout {
  position: relative;
  width: 100%;
  height: 100%;
  user-select: none;
}

.GlobalBody {
  position: fixed;
  top: 60px;
  /* 根据头部高度调整 */
  height: calc(100% - 60px);
  width: 100%;
  display: flex;
}

.GlobalContent {
  flex: 1;
  /* 占据剩余空间 */
  min-width: 0;
  height: 100%;
  position: relative;
}

.GlobalUpload {
  position: absolute;
  right: 20px;
  bottom: 20px;
}
</style>
