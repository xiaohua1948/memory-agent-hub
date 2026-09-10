<template>
  <div v-if="steps.length" class="agent-steps">
    <!-- 标题栏：点击折叠/展开 -->
    <div class="steps-head" @click="expanded = !expanded">
      <i-ep-loading v-if="running" class="head-icon spin" />
      <i-ep-circle-check-filled v-else class="head-icon is-done" />
      <span class="steps-title">{{ title }}</span>
      <span class="steps-toggle">
        {{ expanded ? '收起' : '展开' }}
        <i-ep-arrow-down class="toggle-icon" :class="{ 'is-open': expanded }" />
      </span>
    </div>

    <ul v-show="expanded" class="steps-list">
      <li v-for="s in steps" :key="s.id" :class="['step-item', s.status]">
        <span class="step-mark">
          <i-ep-loading v-if="s.status === 'running'" class="spin" />
          <i-ep-circle-check-filled v-else-if="s.status === 'done'" />
          <i-ep-warning-filled v-else-if="s.status === 'waiting_confirm'" />
          <i-ep-circle-close-filled v-else-if="s.status === 'failed'" />
          <i-ep-more-filled v-else />
        </span>
        <span class="step-text">
          <span class="step-title">{{ s.title || '执行子任务' }}</span>
          <span v-if="s.detail" class="step-detail">{{ s.detail }}</span>
        </span>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

const props = defineProps<{ steps: API.AgentStep[] }>();

const expanded = ref(true);

const running = computed(() => props.steps.some((s) => s.status === 'running'));

/** 已完成（含待确认、失败）的步骤数：终态而非 running 均计入 */
const finished = computed(
  () => props.steps.filter((s) => s.status && s.status !== 'running').length,
);

const title = computed(() => {
  if (running.value) {
    return `正在执行…（${finished.value}/${props.steps.length}）`;
  }
  return `已完成 ${finished.value} 个步骤`;
});

// 有步骤正在执行时自动展开，让用户看到实时进度
watch(running, (v) => {
  if (v) expanded.value = true;
});
</script>

<style scoped>
.agent-steps {
  margin: 6px 0;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  min-width: 240px;
  max-width: 100%;
}

.steps-head {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  font-size: 12px;
}

.head-icon {
  font-size: 14px;
  color: #3d7eff;
}

.head-icon.is-done {
  color: #67c23a;
}

.steps-title {
  flex: 1;
  color: #2b3a55;
  font-weight: 600;
}

.steps-toggle {
  display: flex;
  align-items: center;
  gap: 2px;
  color: #a8abb2;
  font-size: 11px;
}

.toggle-icon {
  font-size: 12px;
  transition: transform 0.2s ease;
}

.toggle-icon.is-open {
  transform: rotate(180deg);
}

.steps-list {
  margin: 8px 0 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.step-mark {
  flex: none;
  font-size: 14px;
  line-height: 18px;
  color: #c0c4cc;
}

.step-item.done .step-mark {
  color: #67c23a;
}

.step-item.running .step-mark {
  color: #3d7eff;
}

.step-item.waiting_confirm .step-mark {
  color: #e6a23c;
}

.step-item.failed .step-mark {
  color: #f56c6c;
}

.step-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.step-title {
  font-size: 12px;
  color: #2b3a55;
  line-height: 18px;
  word-break: break-word;
}

.step-item.running .step-title {
  color: #3d7eff;
}

.step-detail {
  font-size: 11px;
  color: #a8abb2;
  line-height: 16px;
  word-break: break-word;
}

.spin {
  animation: step-spin 1s linear infinite;
}

@keyframes step-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
