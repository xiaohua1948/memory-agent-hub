<template>
  <div id="app">
    <router-view />
    <GlobalImageViewer />
  </div>
</template>

<script setup lang="ts">
import GlobalImageViewer from '@/components/preview/GlobalImageViewer.vue'
</script>
<style>
html,
body {
  margin: 0;
  border: 0;
  width: 100%;
  height: 100%;
  position: relative;
}
#app {
  width: 100%;
  height: 100%;
  position: relative;
}
</style>
