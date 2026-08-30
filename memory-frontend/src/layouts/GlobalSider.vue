<template>
  <div id="globalSider">

    <el-menu text-color="#333333" active-text-color="#3B75FF" :default-openeds="openeds"
      :default-active="currentRouteName" :collapse="isCollapse" class="global-sider-menu" mode="vertical"
      @select="handleSelect">
      <el-menu-item index="0" @click="handleClick">

        <el-icon>
          <Fold v-if="isCollapse" />
          <Expand v-else />
        </el-icon>

      </el-menu-item>

      <el-sub-menu index="1">
        <template #title>
          <el-icon>
            <i-ep-picture />
          </el-icon>
          <span>图片</span>
        </template>
        <el-menu-item index="home">
          <el-icon>
            <i-ep-picture />
          </el-icon>
          <template #title>
            <span>全部图片</span>
          </template>
        </el-menu-item>
        <el-menu-item index="recent">
          <el-icon>
            <i-carbon-recently-viewed />
          </el-icon>
          <template #title>
            <span>最近上传</span>
          </template>
        </el-menu-item>
        <el-menu-item index="brush">
          <el-icon>
            <i-ep-brush />
          </el-icon>
          <template #title>
            <span>清理图片</span>
          </template>
        </el-menu-item>
        <el-menu-item index="ai">
          <el-icon>
            <i-ep-MostlyCloudy />
          </el-icon>
          <template #title>
            <span>AI 处理</span>
          </template>
        </el-menu-item>




      </el-sub-menu>
      <el-sub-menu index="2">

        <template #title>
          <el-icon>
            <i-solar-album-broken />
          </el-icon>
          <span>相册</span>
        </template>
        <el-menu-item index="album">
          <el-icon>
            <i-solar-album-broken />
          </el-icon>
          <template #title>
            <span>相册</span>
          </template></el-menu-item>
        <el-menu-item index="person"> <template #title>
            <el-icon>
              <i-ep-user />
            </el-icon>
            <span>人物</span>
          </template></el-menu-item>
        <el-menu-item index="location">
          <el-icon>
            <i-ep-location />
          </el-icon>
          <template #title>
            <span>地点</span>
          </template></el-menu-item>
        <el-menu-item index="model"> <el-icon>
            <i-ep:iphone />
          </el-icon><template #title>
            <span>设备</span>
          </template></el-menu-item>
      </el-sub-menu>

   
      <el-sub-menu index="5">
        <template #title>
          <el-icon>
            <i-ep-DataAnalysis/>
          </el-icon>
          <span>可视化</span>
        </template>
        <el-menu-item index="fileVisual">
          <el-icon>
            <i-ep-files />
          </el-icon>
          <template #title>
            <span>文件类型</span>
          </template>
        </el-menu-item>
        <el-menu-item index="locationVisual">
          <el-icon>
            <i-ep-location />
          </el-icon>
          <template #title>
            <span>照片足迹</span>
          </template>
        </el-menu-item>
        <el-menu-item index="tagVisual">
          <el-icon>
            <i-ep-PriceTag />
          </el-icon>
          <template #title>
            <span>图片标签</span>
          </template>
        </el-menu-item>
      </el-sub-menu>


      <el-menu-item index="recycle"> <el-icon>
          <i-ep-delete />
        </el-icon> <template #title>

          <span>回收站</span>
        </template></el-menu-item>

      <el-menu-item index="record"> <el-icon>
          <i-ep-Document />
        </el-icon><template #title>

          <span>操作记录</span>
        </template></el-menu-item>
    </el-menu>

  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const route = useRoute()
const currentRouteName = route.name as string

const openeds = ref([]);
const isCollapse = ref(false);
const handleSelect = (key: string, keyPath: string[]) => {
  if (key != '0') {
    router.push({
      name: key,
    })
  }

};

const handleClick = (event: any) => {
  event;
  isCollapse.value = !isCollapse.value
}
</script>
<style scoped>
#globalSider {
  height: 100%;
  background: #ffffff;
  box-shadow: 2px 0 8px 0 rgba(0, 0, 0, 0.06);
  border-right: 1px solid #f0f0f0;
}

/* 菜单宽度设置 */
.global-sider-menu:not(.el-menu--collapse) {
  width: 150px;
}
</style>

/*
.global-sider-menu {
border-right: none;
--el-menu-bg-color: #ffffff;
--el-menu-hover-bg-color: rgba(59, 117, 255, 0.08);
--el-menu-active-color: #3B75FF;
}


:deep(.el-sub-menu__title) {
font-weight: 500;
color: #333;
font-size: 14px;
padding-left: 20px !important;
}

:deep(.el-menu-item) {
font-weight: 500;
color: #666;
padding-left: 48px !important;
font-size: 15px;
}

:deep(.el-menu-item.is-active) {
background: rgba(59, 117, 255, 0.08);
color: #3B75FF !important;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
background: rgba(59, 117, 255, 0.05);
}

:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
font-size: 18px;
margin-right: 12px;
vertical-align: middle;
}

:deep(.el-menu-item.is-active .el-icon) {
color: #3B75FF;
}

:deep(.el-divider) {
margin: 12px 0;
border-color: #f0f0f0;
}
*/
/* 二级菜单缩进调整 */
/* :deep(.el-menu--inline) {
padding: 0 8px !important;
}

:deep(.el-menu--inline .el-menu-item) {
padding-left: 40px !important;
} */
