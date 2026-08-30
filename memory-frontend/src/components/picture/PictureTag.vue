<template>
    <el-dialog :append-to-body="true" v-model="showTag" :close-on-click-modal="false" :modal="false" title="照片类型标签"
        width="400" @closed="close" center>
        <div style="height: 300px;margin: 10px;overflow: auto;">
            <el-tag v-for="(tag, index) in dynamicTags" :key="index" :type="TagTypes[index % 5]" closable
                :disable-transitions="false" @close="handleClose(tag)" size="large" style="margin: 5px;">
                {{ tag }}
            </el-tag>
            <el-input v-if="inputVisible" ref="InputRef" v-model="inputValue" style="width:90px;margin: 5px;"
                @keyup.enter="handleInputConfirm" @blur="handleInputConfirm" />
            <el-button v-else @click="showInput" style="margin: 5px;">
                + 新建标签
            </el-button>
        </div>
    </el-dialog>
</template>

<script lang="ts" setup>
import { nextTick, ref, onMounted, watch } from 'vue'
import { ElInput, } from 'element-plus'
import type { InputInstance } from 'element-plus'
import { selectTagByFileId, addPictureTag, removePictureTag } from '@/api/file/file'
import $bus from '@/utils/bus.ts'
interface Props {
    show: boolean,
    currentFileId: string,
    checkedImages: string[][],
}
const props = withDefaults(defineProps<Props>(), {
    show: false,
    currentFileId: '',
})
const showTag = ref(props.show);
const inputValue = ref('')
const TagTypes = ['primary', 'success', 'info', 'warning', 'danger'] as const;
type TagType = typeof TagTypes[number]; // 生成联合类型
const dynamicTags = ref<string[]>([])
const inputVisible = ref(false)
const InputRef = ref<InputInstance>()
const fileIds = ref(props.checkedImages.flat());
const currentFileId = ref(props.currentFileId);
const handleClose = async (tag: string) => {
    let data = [];
    if (currentFileId.value == '-1') {
        if (fileIds.value.length == 0)
            return;
        data = fileIds.value;
    }
    else {
        data = [currentFileId.value];
    }
    const res = await removePictureTag({
        fileIds: data,
        tag: tag
    })
    if (res.code == 200) {
        ElMessage.success('删除成功');
        dynamicTags.value.splice(dynamicTags.value.indexOf(tag), 1)
    }

}
onMounted(() => {
    handleSelectAllTags();
})
watch(
  // 使用数组包装多个监听源
  [() => props.show, () => props.currentFileId],
  // 参数解构为数组形式
  ([newShow, newCurrentId], [oldShow, oldCurrentId]) => {
    fileIds.value = props.checkedImages.flat();
    currentFileId.value = newCurrentId;
    showTag.value = newShow;
    if (newShow) {
        handleSelectAllTags();
    }
  }
);
const handleSelectAllTags = async () => {

    const res = await selectTagByFileId({
        fileId: currentFileId.value,
    })
    if (res.code == 200) {
        dynamicTags.value = res.data
    }
}
const showInput = () => {
    inputVisible.value = true
    nextTick(() => {
        InputRef.value!.input!.focus()
    })
}

const handleInputConfirm = async () => {
    if (inputValue.value) {
        if (dynamicTags.value.includes(inputValue.value)) {
            ElMessage.warning('标签已存在');
            return;
        }

        let data = [];
        if (currentFileId.value == '-1') {
            if (fileIds.value.length == 0)
                return;
            data = fileIds.value;
        }
        else {
            data = [currentFileId.value];
        }
        const res = await addPictureTag({
            fileIds: data,
            tagName: inputValue.value
        })
        if (res.code == 200) {
            dynamicTags.value.push(inputValue.value)
            ElMessage.success('添加成功');
        }
        else {
            ElMessage.error('添加失败');
        }
    }
    inputVisible.value = false
    inputValue.value = ''
}
const close = () => {
    $bus.emit("closePictureTag", false);
    // 修改完不能立马回传子组件  
}
</script>