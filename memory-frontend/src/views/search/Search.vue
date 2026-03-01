<template>
  <div id="search-container">
    <el-breadcrumb :separator-icon="ArrowRight" class="breadcrumb">
      <el-breadcrumb-item class="breadcrumb-item1" :to="{ path: '/home' }">返回首页</el-breadcrumb-item>
      <el-breadcrumb-item>搜索图片</el-breadcrumb-item>
    </el-breadcrumb>
    <div class="search-container-main">
      <div class="search-container-item search-input-item">
        <el-input @input="handleChangeInput" @keyup.enter="" v-model="searchKeyword" placeholder="输入关键词搜索照片"
          class="search-input">
          <template #prefix>
            <el-icon>
              <search />
            </el-icon>
          </template>
        </el-input>
      </div>
      <div class="search-container-item" v-if="isSearching && (!isEmpty)">
        <div class="search-container-search-res">{{ searchKeyword }}分类</div>
      </div>
      <div v-if="(!isSearching)">
        <div class="search-container-item">
          <div class="search-container-item-title">分类</div>

          <el-tag v-for="(tag, index) in Tags" :key="index" class="tag-item" effect="plain" type="info" size="large"
            @click="handleSearchPicture('tag', tag.tagName)">
            {{ tag.tagName }}
          </el-tag>

        </div>
        <div class="search-container-item">
          <div class="search-container-item-title">地点</div>

          <el-tag v-for="(location, index) in locations" :key="index" class="tag-item" effect="plain" type="info"
            size="large" @click="handleSearchPicture('location', location.city)">
            <div style="display: flex;">
              <i-ep-location style="margin-right:2px" />
              {{ location.city }}
            </div>

          </el-tag>

        </div>
        <div class="search-container-item">
          <div class="search-container-item-title">设备</div>
          <el-tag v-for="(model, index) in models" :key="model" class="tag-item" effect="plain" type="info" size="large"
            @click="handleSearchPicture('model', model)">
            <div style="display: flex;">
              <i-ep-iphone style="margin-right:2px" />
              {{ model }}
            </div>
          </el-tag>
        </div>
      </div>
      <el-empty v-if="isEmpty" description="没有搜索结果"></el-empty>
    </div>

  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router';
import { Search } from '@element-plus/icons-vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { selectAllTags, selectAllLocations, selectAllModels } from '@/api/search/search'
// 标签数据（示例数据）
const router = useRouter();
interface Tag {
  tagName: string;
  count: number;
}
interface Location {
  city: string;
}
const Tags = ref<Tag[]>([]);

const locations = ref<Location[]>([])
const models = ref([
])
const isSearching = ref(false);
const isEmpty = ref(false);
const searchType = ref('');
const searchKeyword = ref('');
// 响应式数据
const handleSearchPicture = (searchType: string, searchKeyword: string) => {
  router.push({
    path: '/searchPicture',
    query: {
      searchType: searchType,
      searchKeyword: searchKeyword
    },
  });
}
onMounted(() => {
  handleSelectAlltags();
  handleSelectAllLoations();
  handleSelectAllModels();
})
const handleSelectAlltags = async () => {
  const res = await selectAllTags()
  if (res.code == 200) {
    Tags.value = res.data;
  }
}
const handleSelectAllLoations = async () => {
  const res = await selectAllLocations();
  if (res.code == 200) {
    locations.value = res.data;
  }
}
const handleSelectAllModels = async () => {
  const res = await selectAllModels();
  if (res.code == 200) {
    models.value = res.data;
  }
}
// 搜索触发逻辑
const handleSearch = () => {
  const keyword = searchKeyword.value;
  Tags.value.forEach((tag) => {
    if (tag.tagName === keyword) {
      isEmpty.value = false;
      return;
    }
  });
  locations.value.forEach((location) => {
    if (location.city === keyword) {

      isEmpty.value = false;
      return;
    }
  })
  models.value.forEach((model) => {
    if (model === keyword) {

      isEmpty.value = false;
      return;
    }
  })

  if (isEmpty.value == false) {
    isSearching.value = true;
    return;
  }
  isSearching.value = true;
  isEmpty.value = true;

}
const handleChangeInput = () => {
  if (searchKeyword.value.length <= 0) {
    isSearching.value = false;
  }
}
</script>
<style scoped>
#search-container {
  width: 100%;
  height: 100%;
  overflow: auto;
  display: flex;
  justify-content: center;
  background-color: #f4f4f4;
}

.search-container-main {
  width: 600px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.search-container-item {
  background: #fff;
  padding: 20px;
  margin-bottom: 20px;
  user-select: none;
}

.search-container-search-res {
  color: #333;
  font-weight: 500;
  font-size: 14px;

}

.search-container-item-title {
  margin-left: 5px;
  margin-bottom: 16px;
  font-size: 18px;
  line-height: 16px;
  color: #333;
  font-weight: 500;
}

.search-input-item {
  background: 0;
  width: 100%;
  height: 40px;
  padding: 0;
  padding-top: 20px;
}

.search-input {
  height: 100%;
}

:deep(.el-input__wrapper) {
  border-radius: 10px;
}

.tag-item {
  user-select: none;
  cursor: pointer;
  margin: 5px;
}













.breadcrumb {
  position: absolute;
  left: 20px;
  top: 20px;
}


.breadcrumb-item1 {
  user-select: none;
  cursor: pointer;
  color: rgb(59, 117, 255);
  white-space: nowrap;
}

:deep(.el-breadcrumb__inner) {
  color: rgb(59, 117, 255);
  font-weight: 400;
  user-select: none;
  font-size: 16px;
}
</style>