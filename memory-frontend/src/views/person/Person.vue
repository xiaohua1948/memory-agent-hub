<template>
    <div id="album">
        <!-- 顶部菜单 -->
        <div class="content-header">
            <div class="content-header-left">
                <el-dropdown placement="top-start" trigger="click" @command="handleCommandDisplay"
                    @visible-change="(val: boolean) => { displayIcon = !val; }">
                    <div class="content-header-left-span1">{{
                        pageRequest.display ? '人物' : '隐藏' }}
                        <i-ep-CaretBottom class="content-header-display" v-if="displayIcon"></i-ep-CaretBottom>
                        <i-ep-CaretTop class="content-header-display" v-else></i-ep-CaretTop>
                    </div>

                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item class="content-header-right-item"
                                :style="{ color: pageRequest.display == item.value ? '#409EFF' : '' }"
                                :icon="pageRequest.display == item.value ? Check : ''" :command="index"
                                v-for="(item, index) in displayList">{{ item.label
                                }}</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
                <span class="content-header-left-span2">你照片一共拥有<span style="color: #3b75ff;">{{ PersonNumber
                }}</span>个人物</span>
            </div>
            <div class="content-header-right">
                <el-button text class="content-header-right-button" @click="showCheck = true">
                    <i-ep-edit />
                    <span class="content-header-button-span">操作</span>
                </el-button>
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
            </div>
        </div>
        <el-divider />
        <!-- 顶部选中菜单 -->
        <div id="checked-menu" v-if="showCheck">
            <div class="checked-menu-item">
                <el-icon class="checked-menu-icon-text close-icon">
                    <i-ep:CloseBold @click="closecheckedMenu()" />
                </el-icon>
                <span class="checked-menu-icon-text checked-menu-item-select">已选择{{ checkedPersonIds.length }}个人物</span>
                <el-button color="#3174ff" @click="handleCheckAll">
                    <span class="icon-text">{{ isCheckAllText }}</span>
                </el-button>
            </div>

            <div class="checked-menu-feature">

                <el-button @click="handleMergePerson" color="#3174ff" class="checked-menu-button"
                    :disabled="checkedPersonIds.length < 2"> <el-icon class="icon-text">
                        <i-ep:connection />
                    </el-icon><span class="icon-text">合并人物</span>
                </el-button>

                <el-button @click="handleHiddenPerson" color="#3174ff" class="checked-menu-button"> <el-icon
                        class="icon-text">
                        <i-ep:hide />
                    </el-icon><span class="icon-text">隐藏人物</span>
                </el-button>
            </div>
        </div>


        <!-- 相册列表显示 -->
        <div id="picture-list" @scroll="handleScroll">
            <div class="picture-list-item" v-for="(relation) in relationCategoryList">
                <div class="picture-person-header" v-if="relation.count > 0">
                    <div class="relate-name">{{ relation.label }}</div>
                    <div data-v-3bf4dce4="" class="relate-name-count">共<span style="color: #3b75ff;">{{ relation.count
                    }}位</span></div>
                </div>

                <div class="picture-group-content">
                    <div class="picture-group-album" v-for="(item, index) in PersonAlbumList" :key="index">
                        <div v-if="relationCategory(item.personRelation) == relation.label"
                            :class="{ 'addPersonCheckArea': showCheck, 'picture-group-item': true }"
                            :style="{ '--PersonCheckAreaWidth': (imageAdjustSize + 40) + 'px', '--PersonCheckAreaHeight': (imageAdjustSize + 60) + 'px' }">

                            <el-checkbox-group v-if="showCheck" class="picture-group-checkbox"
                                v-model="checkedPersonIds" @change="handleCheckedPerson">
                                <el-checkbox :key="index" :value="item.personId" size="large">
                                </el-checkbox>
                            </el-checkbox-group>
                            <el-image @click="handleClickPersonAlbum(item.personId)" :style="pictureSacleStyle"
                                :src="item.coverUrl || defaultCover" class="picture-group-image">
                            </el-image>
                            <div v-if="item.coverUrl" class="album-cover-preview" @click.stop="previewCover(item)">
                                <i-ep-zoom-in />
                            </div>
                            <div class="picture-item-combination" v-if="personNameInput[index]">
                                <el-input maxlength="10" type="text" :ref="(el) => setInputRef(el, index)"
                                    @keyup.enter="handleUpdatePersonName(item, index)"
                                    style="width: 100px;border-right: 0;height: 25px;" size="small"
                                    v-model="currentSetPersonName" placeholder="添加名字" @blur="handleBlur(index)">

                                    <template #append>
                                        <el-button :icon="Select"
                                            :style="{ color: currentSetPersonName.length > 0 ? '#3b75ff' : '' }"
                                            @click="handleUpdatePersonName(item, index)" />
                                    </template>
                                </el-input>
                            </div>

                            <div class="picture-item-combination" v-else
                                @click="showPersonNameInput(index, item.personName)">
                                <span
                                    :class="{ 'picture-item-name': true, 'picture-item-hasName': item.personName != null }">
                                    {{
                                        item.personName == null ? "添加名字" : item.personName }}
                                </span>
                            </div>

                        </div>
                    </div>
                </div>
            </div>


            <el-empty v-if="PersonAlbumList.length == 0" description="暂无数据">
            </el-empty>
        </div>




    </div>

</template>
<script setup lang="ts">
/******************************​ 依赖导入 ​******************************/
import { onMounted, ref, computed, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Check, Select } from '@element-plus/icons-vue'
import { selectAllPersonAlbum, updatePersonName, mergePerson, hiddenPerson } from '@/api/person/person'
import defaultCover from '@/assets/image/album.png';
import $bus from '@/utils/bus.ts'


/******************************​ 配置项 ​******************************/


// 图片样式配置
const imageStyleSizeList = [
    { label: '大图模式', value: 100 },
    { label: '中图模式', value: 90 },
    { label: '小图模式', value: 80 },
]
// 路由
const router = useRouter();


// 分页相关
const pageRequest = ref({
    current: 1,
    size: 50,
    display: true,
})
const displayIcon = ref(true);
const displayList = ref([
    { label: '人物', value: true },
    { label: '隐藏', value: false },
])


const imageStyleSize = ref(imageStyleSizeList[2].value)
const imageStyleText = ref(imageStyleSizeList[2].label)
const imageAdjustSize = ref(imageStyleSize.value)
const personNameInput = ref<boolean[]>([]);
const currentSetPersonName = ref('');
const inputRefs = ref<any[]>([])
const showCheck = ref(false);
type RelationCategory = '家人' | '朋友' | '同学' | '其他';
const relationCategoryList = ref([{
    label: '家人',
    count: 0
},
{
    label: '朋友',
    count: 0
},
{
    label: '同学',
    count: 0
},
{
    label: '其他',
    count: 0
}
]);
// 映射关系（根据中文显示值映射）
const RELATION_CATEGORY_MAP: Record<string, RelationCategory> = {
    '我自己': '家人',
    '亲爱的': '家人',
    '孩子': '家人',
    '爸爸': '家人',
    '妈妈': '家人',
    '朋友': '朋友',
    '同学': '同学',
    '同事': '朋友',
    '其他亲属': '家人',
    '无关系': '其他'
};

// 在Vue组件中
const relationCategory = ((personRelation: string) => {
    // 这里假设 relation 是来自 props 或响应式变量
    return RELATION_CATEGORY_MAP[personRelation] || '其他';
});

// 加载状态
const isLoading = ref(false)
const hasMore = ref(true)

// 选择相关状态
const pictureSacleStyle = computed(() => ({
    width: imageAdjustSize.value + 'px',
    height: imageAdjustSize.value + 'px',
}))
const closecheckedMenu = () => {
    checkedPersonIds.value = [];
    isCheckAll.value = false;
    showCheck.value = false
    isCheckAllText.value = '全选'
}
const checkedPersonAll = ref<number[]>([]);
const checkedPersonIds = ref<number[]>([]);
// 数据相关
const PersonAlbumList = ref<API.PersonAlbum[]>([])
const PersonNumber = computed(() => {
    let count = [0, 0, 0, 0, 0];
    checkedPersonAll.value = [];
    PersonAlbumList.value.forEach(item => {

        checkedPersonAll.value.push(item.personId);
        relationCategoryList.value.forEach((relation, index) => {
            if (relationCategory(item.personRelation) == relation.label) {
                count[index] += 1;
            }
        })

    })

    relationCategoryList.value.forEach((relation, index) => {
        relation.count = count[index];
    })

    return PersonAlbumList.value.length;
})

/******************************​ 生命周期钩子 ​******************************/
let observer: ResizeObserver | null = null

onMounted(() => {
    handleselectAllPersonAlbum()
    setupResizeObserver()
})

onUnmounted(() => {
    if (observer) observer.disconnect()
})

/******************************​ 事件处理函数 ​******************************/

const handleCheckedPerson = (checkedIds: number[]) => {
    checkedPersonIds.value = checkedIds
}

const isCheckAll = ref(false)
const isCheckAllText = ref('全选')
// // 全选/反选处理
const handleCheckAll = () => {
    // 切换全选状态
    if (isCheckAll.value) {
        // 执行取消勾选
        closecheckedMenu();
    }
    else {
        // 执行全选
        checkedPersonIds.value = checkedPersonAll.value;

        isCheckAll.value = true;
        isCheckAllText.value = '取消勾选';

    }
}
// 合并人物
const handleMergePerson = async () => {
    if (checkedPersonIds.value.length < 2 ) {
        ElMessage.warning("请选择合适的数据")
        return;
    }
    const res = await mergePerson({
        personIds: checkedPersonIds.value
    })
    if (res.code === 200) {
        ElMessage.success("合并成功");
        closecheckedMenu();
        handleselectAllPersonAlbum()
    }
}
// 隐藏人物
const handleHiddenPerson = async () => {
    if (checkedPersonIds.value.length < 1) {
        ElMessage.warning("请选择合适的数据")
        return;

    }
    const res = await hiddenPerson({
        personIds: checkedPersonIds.value
    })
    if (res.code === 200) {
        ElMessage.success("隐藏成功");
        closecheckedMenu();
        handleselectAllPersonAlbum()
    }
}
const handleClickPersonAlbum = (personId: number) => {

    router.push({
        name: 'personAlbum',
        params: { personId: personId }
    })
}
// 预览人物相册封面（全屏）
const previewCover = (item: API.PersonAlbum) => {
    if (!item.coverUrl) return
    $bus.emit('openImageViewer', { urlList: [item.coverUrl], initialIndex: 0 })
}
// 设置输入框引用
const setInputRef = (el: any, index: number) => {
    inputRefs.value[index] = el
}
//显示修改名字的input
const showPersonNameInput = async (index: number, personName: string) => {
    currentSetPersonName.value = personName || "";
    personNameInput.value[index] = true;
    // 自动聚焦并选中文本
    handleFocus(index);
}
const handleFocus = async (index: number) => {
    await nextTick()
    if (inputRefs.value[index]) {
        const inputEl = inputRefs.value[index]
        inputEl.focus()
    }
}
const handleBlur = (index: number) => {

    setTimeout(() => {
        personNameInput.value[index] = false;
    }, 200);
}
//修改名字
const handleUpdatePersonName = async (item: API.PersonAlbum, index: number) => {
    // 自动聚焦并选中文本
    handleFocus(index);

    item.personName = currentSetPersonName.value;
    personNameInput.value[index] = true;
    const res = await updatePersonName({
        personId: item.personId,
        faceId: item.faceId,
        personName: item.personName
    })
    if (res.code === 200) {
        ElMessage.success("修改成功");
    }
    else {
        ElMessage.error("修改失败")
    }
    personNameInput.value[index] = false;
}
const handleCommandDisplay = (index: number) => {
    pageRequest.value.display = displayList.value[index].value
    handleselectAllPersonAlbum();
}
// 图片尺寸变化
const handleCommandImageSize = (index: number) => {
    imageStyleSize.value = imageStyleSizeList[index].value
    imageStyleText.value = imageStyleSizeList[index].label
    adjustImageSizeToContainer()
}



// 滚动加载处理
const handleScroll = (e: Event) => {
    const container = e.target as HTMLElement
    if (container.scrollHeight - (container.scrollTop + container.clientHeight) <= 1) {
        if (!isLoading.value && hasMore.value) loadMoreData()
    }
}





/******************************​ 工具函数 ​******************************/


// 调整图片尺寸逻辑
const adjustImageSize = (containerWidth: number) => {
    const count = Math.max(1, Math.floor(containerWidth / (imageStyleSize.value + 20)))
    imageAdjustSize.value = Math.floor(containerWidth / count) - 20
}

// 防抖函数
const debounce = (fn: Function, delay = 100) => {
    let timer: number | null = null
    return (...args: any[]) => {
        timer && clearTimeout(timer)
        timer = setTimeout(() => fn(...args), delay)
    }
}



/******************************​ 数据请求 ​******************************/
// 获取相册列表
const handleselectAllPersonAlbum = () => {
    selectAllPersonAlbum(pageRequest.value).then(res => {
        if (res.code === 200) {

            PersonAlbumList.value = res.data.records

        } else {
            ElMessage.error(res.message)
        }
    })
}

// 加载更多数据
const loadMoreData = async () => {
    isLoading.value = true
    pageRequest.value.current += 1
    try {
        const res = await selectAllPersonAlbum(pageRequest.value)
        if (res.code === 200) {
            PersonAlbumList.value = [...PersonAlbumList.value, ...res.data.records]
            hasMore.value = pageRequest.value.current < res.data.pages

        }
    } finally {
        isLoading.value = false
    }
}




// 初始化ResizeObserver
const setupResizeObserver = () => {
    const container = document.getElementById('picture-list')
    if (!container) return

    const debouncedAdjust = debounce((width: number) => adjustImageSize(width))
    observer = new ResizeObserver(entries => {
        debouncedAdjust(entries[0].contentRect.width - 20)
    })
    observer.observe(container)
}

// 容器尺寸调整
const adjustImageSizeToContainer = () => {
    const container = document.getElementById('picture-list')
    container && requestAnimationFrame(() => adjustImageSize(container.getBoundingClientRect().width - 20))
}
</script>


<style scoped>
#album {
    width: 100%;
    height: 100%;
}

.content {
    width: 100%;
    height: 100%;
    position: relative;
}

.content-header {
    z-index: 4;
    width: 100%;
    height: 60px;
    display: flex;
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
    user-select: none;
}

/* 左边文字 */
.content-header-left-span1 {
    width: 50px;
    margin-left: 20px;
    font-size: large;
    white-space: nowrap;
    font-size: 20px;
    font-weight: 700;
    user-select: none;

    align-items: center;

    cursor: pointer;
}

/* 是否展示隐藏人物 */
.content-header-display {
    vertical-align: middle;
    font-size: 18px;
    user-select: none;
    cursor: pointer;
}

.content-header-left-span2 {
    margin-left: 22px;
    font-size: 12px;
    font-weight: 700;
    white-space: nowrap;
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




/* 顶部选中菜单 */
#checked-menu {
    background-color: #3174ff;
    height: 60px;
    width: 100%;
    z-index: 10;
    position: absolute;
    top: 0;
    display: flex;
    align-items: center;

}

.checked-menu-item {
    margin-left: 20px;
    display: flex;
    align-items: center;
    margin-right: auto;
}

.checked-menu-item-select {
    margin-left: 10px;
    width: 120px;
}

.checked-menu-feature {
    display: flex;
    align-items: center;
    margin-right: 20px;
}

.checked-menu-button {
    padding: 5px;
}

.checked-menu-icon-text {
    font-size: 1rem;
    color: white;
}

.close-icon {
    font-size: 22px;
}

.close-icon:hover {

    background-color: #ca4646;
    border-radius: 50%;
    cursor: pointer;
}


/* 图片列表 */
#picture-list {
    margin-left: 20px;
    width: calc(100% - 20px);
    height: calc(100% - 80px);

    overflow-y: auto;
    overflow-x: hidden;
}

.picture-person-header {
    display: flex;
    font-size: 14px;
    line-height: 20px;
    margin-top: 10px;
    margin-left: 10px;
    font-family: PingFangSC-Semibold;
    color: #666;
    user-select: none;
}

.relate-name {
    padding-right: 18px;
}

.relate-name-count {
    font-size: 12px;
}

.picture-group-content {
    margin-top: 10px;
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
}

.picture-group-album {
    position: relative;
}

.picture-group-item {
    margin: 10px;
    margin-top: 0px;
    margin-bottom: 0px;
    position: relative;


}

.picture-group-item:hover {
    border-radius: 10px;
    background: #f8f8f8;
    cursor: pointer;
}

.picture-group-item:hover .picture-item-name {
    visibility: visible;
}

.picture-group-checkbox {
    position: absolute;
    /* display: none; */
    top: 0;
    left: 10px;
    z-index: 2;
    border-radius: 50%;

}

.picture-group-image {
    transition: all 0.3s ease;
    border-radius: 50%;
    margin: 20px;
    margin-bottom: 0px;
}

.picture-item-combination {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 30px;
    padding-bottom: 10px;
}

.picture-item-name {
    width: 100px;
    font-size: 13px;
    color: #c9c9c9;
    text-align: center;
    cursor: pointer;
    user-select: none;
    white-space: nowrap;
    visibility: hidden;
    overflow: hidden;
    text-overflow: ellipsis;
}

.picture-item-hasName {
    visibility: visible;
    color: black;
}

.picture-item-name:hover {
    color: #3b75ff;
}

:deep(.el-input-group__append) {
    background-color: 0;
    border: 0;
    padding: 0;
    width: 80px;

}

:deep(.el-checkbox .el-checkbox__inner) {
    z-index: 3;
    border-radius: 50%;
    width: 18px;
    height: 18px;

}

:deep(.el-checkbox__inner) {
    background-color: transparent;
    border: 2px solid #9c9b9b;
}

:deep(.el-checkbox__inner::after) {
    border: 2px solid #9c9b9b;
    border-left: 0;
    border-top: 0;
    left: 5px;
    top: 2px;
}
</style>
<style>
.addPersonCheckArea span span::before {
    content: "";
    position: absolute;
    top: -13.3px;
    left: -13.3px;
    width: var(--PersonCheckAreaWidth);
    height: var(--PersonCheckAreaHeight);
    cursor: pointer;
    z-index: 3;
}
</style>
