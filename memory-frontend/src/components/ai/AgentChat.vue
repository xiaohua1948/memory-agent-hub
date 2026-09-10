<template>
  <div class="agent-chat">
    <!-- 右下角浮动按钮 -->
    <div
      class="agent-fab"
      :class="{ active: panelVisible }"
      @click="togglePanel"
      title="AI 智能助手"
    >
      <i-ep-close v-if="panelVisible" class="fab-icon" />
      <i-ri:robot-2-fill v-else class="fab-icon" />
    </div>

    <!-- 展开面板 -->
    <transition name="agent-slide">
      <div
        v-if="panelVisible"
        class="agent-panel"
        :style="panelStyle"
        :class="{ 'is-interacting': dragging || resizing }"
      >
        <!-- 顶栏（可拖动） -->
        <div class="agent-panel-header" @mousedown="startDrag">
          <div class="panel-title">
            <i-ri:robot-2-line class="title-icon" />
            <span>AI 智能助手</span>
          </div>
          <div class="panel-ops" @mousedown.stop>
            <el-tooltip content="新建会话" placement="bottom">
              <el-button link @click="handleNewSession">
                <i-ep-plus class="op-icon" />
              </el-button>
            </el-tooltip>
            <el-tooltip content="关闭" placement="bottom">
              <el-button link @click="closePanel">
                <i-ep-close class="op-icon" />
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 会话切换（任务路由统一由主 Agent 判别，不再区分 agent 类型） -->
        <div class="agent-toolbar">
          <el-select
            v-model="currentSessionId"
            class="session-select"
            placeholder="选择会话"
            size="small"
            @change="handleSwitchSession"
          >
            <el-option
              v-for="s in sessions"
              :key="s.sessionId"
              :label="sessionLabel(s)"
              :value="s.sessionId"
            />
          </el-select>
          <el-tooltip content="删除当前会话" placement="top">
            <el-button
              link
              class="session-delete-btn"
              :disabled="!currentSessionId"
              @click="handleDeleteSession"
            >
              <i-ep-delete class="op-icon" />
            </el-button>
          </el-tooltip>
        </div>

        <!-- 消息列表 -->
        <div ref="messagesRef" class="agent-messages">
          <div v-if="messages.length === 0" class="agent-empty">
            <i-ri:robot-2-line class="empty-icon" />
            <p>你好，我是 AI 智能助手 🦐</p>
            <p class="empty-tip">可以帮你查找照片、整理相册，或生成网页</p>
          </div>

          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['agent-msg', msg.role === 'user' ? 'is-user' : 'is-ai']"
          >
            <!-- 用户消息 -->
            <template v-if="msg.role === 'user'">
              <div v-if="msg.content" class="bubble-wrap">
                <div class="bubble user-bubble">{{ msg.content }}</div>
                <el-tooltip content="复制文本" placement="top">
                  <span class="copy-btn" @click.stop="copyText(msg.content)">
                    <i-ep-copy-document class="copy-icon" />
                  </span>
                </el-tooltip>
              </div>
              <div v-if="msg.thumbnails?.length" class="msg-thumbs">
                <img
                  v-for="(t, i) in msg.thumbnails"
                  :key="i"
                  :src="t"
                  class="msg-thumb"
                  alt=""
                  @click="previewUrls(msg.thumbUrls || msg.thumbnails, i)"
                />
              </div>
            </template>

            <!-- AI 消息 -->
            <template v-else>
              <!-- 执行步骤条（位于本轮回复内容上方） -->
              <AgentSteps v-if="msg.steps?.length" :steps="msg.steps!" />

              <!-- 文本 -->
              <div v-if="msg.content" class="bubble-wrap">
                <div class="bubble ai-bubble">{{ msg.content }}</div>
                <el-tooltip content="复制文本" placement="top">
                  <span class="copy-btn" @click.stop="copyText(msg.content)">
                    <i-ep-copy-document class="copy-icon" />
                  </span>
                </el-tooltip>
              </div>

              <!-- 照片结果网格 -->
              <div v-if="msg.photos?.length" class="msg-photos">
                <el-image
                  v-for="(p, i) in msg.photos"
                  :key="p.fileId"
                  :src="p.thumbnailUrl"
                  fit="cover"
                  class="msg-photo"
                  :title="p.originFileName"
                  @click="previewPhotos(msg.photos || [], i)"
                />
              </div>

              <!-- 下载卡片 -->
              <div v-if="msg.downloads?.length" class="download-card">
                <div class="download-info">
                  <i-ep-download class="download-icon" />
                  <div class="download-text">
                    <span class="download-title">{{ msg.downloads.length }} 张照片可下载</span>
                    <span class="download-sub">点击按钮下载 ZIP 压缩包</span>
                  </div>
                </div>
                <el-button
                  type="primary"
                  size="small"
                  :loading="msg.downloading"
                  @click="downloadFiles(msg)"
                >
                  {{ msg.downloading ? '打包中…' : '打包下载' }}
                </el-button>
              </div>

              <!-- 问数指标（标量结果，如相册总数） -->
              <div v-if="msg.metric" class="metric-card">
                <span class="metric-value">{{ msg.metric.value }}</span>
                <span class="metric-label">{{ msg.metric.label }}</span>
              </div>

              <!-- 问数表格（明细结果，如相册列表） -->
              <div v-if="msg.table?.rows?.length" class="result-table">
                <table>
                  <thead>
                    <tr>
                      <th v-for="c in msg.table!.columns" :key="c.key">{{ c.label }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, ri) in msg.table!.rows" :key="ri">
                      <td v-for="c in msg.table!.columns" :key="c.key">
                        <img
                          v-if="c.type === 'image' && row[c.key]"
                          :src="row[c.key]"
                          class="table-thumb"
                          alt=""
                          @click="previewUrls([row[c.key]], 0)"
                        />
                        <span v-else>{{ row[c.key] ?? '-' }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 确认卡片 -->
              <div v-if="msg.pendingAction" class="pending-card">
                <div class="pending-header">
                  <i-ep-warning-filled class="pending-icon" />
                  <span class="pending-title">{{ msg.pendingAction.title }}</span>
                </div>
                <div v-if="msg.pendingAction.preview" class="pending-preview">
                  <div v-if="msg.pendingAction.preview.thumbnails?.length" class="pending-thumbs">
                    <img
                      v-for="(t, i) in msg.pendingAction.preview.thumbnails.slice(0, 6)"
                      :key="i"
                      :src="t"
                      class="pending-thumb"
                      alt=""
                      @click="previewUrls(msg.pendingAction.preview.thumbnails, i)"
                    />
                    <span
                      v-if="(msg.pendingAction.preview.count || 0) > 6"
                      class="pending-more"
                    >
                      +{{ (msg.pendingAction.preview.count || 0) - 6 }}
                    </span>
                  </div>
                  <div class="pending-meta">
                    <span v-if="msg.pendingAction.preview.albumName">
                      相册：{{ msg.pendingAction.preview.albumName }}
                    </span>
                    <span
                      v-if="
                        msg.pendingAction.preview.newName &&
                        msg.pendingAction.preview.newName !==
                          msg.pendingAction.preview.oldName
                      "
                    >
                      改名：{{ msg.pendingAction.preview.oldName }} →
                      {{ msg.pendingAction.preview.newName }}
                    </span>
                    <span
                      v-if="
                        msg.pendingAction.preview.newDescription &&
                        msg.pendingAction.preview.newDescription !==
                          msg.pendingAction.preview.oldDescription
                      "
                    >
                      新描述：{{ msg.pendingAction.preview.newDescription }}
                    </span>
                    <span
                      v-if="
                        msg.pendingAction.params?.description &&
                        !['UPDATE_ALBUM', 'DELETE_ALBUM'].includes(
                          msg.pendingAction.type as string,
                        )
                      "
                    >
                      {{ msg.pendingAction.params.description }}
                    </span>
                    <span
                      v-if="msg.pendingAction.preview.deleteWarning"
                      class="pending-danger"
                    >
                      注意：{{ msg.pendingAction.preview.deleteWarning }}
                    </span>
                    <span v-if="msg.pendingAction.preview.count">
                      {{ msg.pendingAction.type === 'REMOVE_PICTURES' ? '移除' : '共' }}
                      {{ msg.pendingAction.preview.count }} 张照片
                    </span>
                  </div>
                </div>
                <div class="pending-actions">
                  <template v-if="msg.actionStatus === 'pending'">
                    <el-button size="small" type="primary" @click="confirmAction(msg)">
                      确认执行
                    </el-button>
                    <el-button size="small" @click="cancelAction(msg)">取消</el-button>
                  </template>
                  <el-tag v-else-if="msg.actionStatus === 'executing'" type="info" size="small">
                    执行中…
                  </el-tag>
                  <el-tag v-else-if="msg.actionStatus === 'done'" type="success" size="small">
                    {{ msg.actionResult || '已完成' }}
                  </el-tag>
                  <el-tag v-else-if="msg.actionStatus === 'error'" type="danger" size="small">
                    执行失败
                  </el-tag>
                </div>
              </div>

              <!-- HTML 预览 -->
              <div v-if="msg.html" class="html-preview">
                <div class="html-toolbar">
                  <span class="html-label">网页预览</span>
                  <div class="html-ops">
                    <el-button link size="small" @click="downloadHtml(msg)">
                      <i-ep-download class="op-icon" /> 下载
                    </el-button>
                    <el-button link size="small" @click="openHtml(msg)">
                      <i-ep-top-right class="op-icon" /> 新窗口
                    </el-button>
                  </div>
                </div>
                <iframe
                  class="html-frame"
                  :srcdoc="msg.html"
                  sandbox="allow-scripts"
                  title="AI 生成网页"
                />
              </div>
            </template>
          </div>

          <!-- 思考中占位 -->
          <div v-if="sending" class="agent-msg is-ai">
            <div class="bubble ai-bubble thinking">
              <span class="dot" /> <span class="dot" /> <span class="dot" />
            </div>
          </div>
        </div>

        <!-- 照片上下文条 -->
        <div v-if="selectedPictures.length" class="agent-context">
          <span class="context-label">上下文照片（{{ selectedPictures.length }}）</span>
          <div class="context-thumbs">
            <div v-for="p in selectedPictures" :key="p.fileId" class="context-item">
              <img
                :src="p.thumbnailUrl"
                class="context-thumb"
                alt=""
                @click="previewUrls([p.fileUrl || p.thumbnailUrl], 0)"
              />
              <span class="context-remove" @click="removePicture(p.fileId)">
                <i-ep-close />
              </span>
            </div>
          </div>
          <el-button link size="small" @click="clearPictures">清空</el-button>
        </div>

        <!-- 输入区 -->
        <div class="agent-input">
          <el-tooltip content="选择照片作为上下文" placement="top">
            <el-button link class="input-pic-btn" @click="openSelectPicture">
              <i-ep-picture class="op-icon" />
            </el-button>
          </el-tooltip>
          <el-input
            v-model="draft"
            type="textarea"
            :rows="1"
            resize="none"
            placeholder="输入你的需求，例如：帮我找北京的照片"
            class="input-textarea"
            @keydown.enter.exact.prevent="send"
          />
          <el-button
            type="primary"
            class="send-btn"
            :disabled="!canSend"
            :loading="sending"
            @click="send"
          >
            <i-ep-promotion v-if="!sending" />
          </el-button>
        </div>

        <!-- 四条边 + 四个角缩放手柄 -->
        <div
          v-for="h in resizeHandles"
          :key="h.dir"
          :class="['resize-handle', `resize-${h.dir}`]"
          :title="h.title"
          @mousedown.stop.prevent="startResize($event, h.dir)"
        >
          <i-ep-full-screen v-if="h.dir === 'se'" class="resize-icon" />
        </div>
      </div>
    </transition>

    <!-- 选照片弹窗（独立事件，避免与图像分类页冲突） -->
    <el-dialog
      v-model="selectVisible"
      title="选择照片"
      width="90%"
      top="4vh"
      :append-to-body="true"
      destroy-on-close
      class="agent-select-dialog"
    >
      <div class="select-wrap">
        <SelectPicture emit-event="agentSelectPicture" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import $bus from '@/utils/bus';
import requestPublicConfig from '@/api/config';
import SelectPicture from '@/components/select_picture/SelectPicture.vue';
import AgentSteps from '@/components/ai/AgentSteps.vue';
import {
  agentExecute,
  createSession,
  deleteSession,
  listMessages,
  listSessions,
  streamChat,
} from '@/api/ai/ai';
import type { AiChatEvent } from '@/api/ai/ai';

// 缩放方向：n/s/e/w 为四边，组合为四角
type ResizeDir = 'n' | 's' | 'e' | 'w' | 'ne' | 'nw' | 'se' | 'sw';

const resizeHandles: { dir: ResizeDir; title: string }[] = [
  { dir: 'n', title: '上下拉伸' },
  { dir: 's', title: '上下拉伸' },
  { dir: 'e', title: '左右拉伸' },
  { dir: 'w', title: '左右拉伸' },
  { dir: 'ne', title: '自由缩放' },
  { dir: 'nw', title: '自由缩放' },
  { dir: 'se', title: '自由缩放' },
  { dir: 'sw', title: '自由缩放' },
];

interface ChatMsg {
  id: string;
  role: 'user' | 'assistant';
  content?: string;
  thumbnails?: string[];
  thumbUrls?: string[];
  photos?: API.AiPhotoResult[];
  downloads?: API.AiDownloadItem[];
  downloading?: boolean;
  /** 问数指标（metric 事件，如「相册数量：12」） */
  metric?: API.AiMetricPayload;
  /** 问数表格（table 事件，如相册明细列表） */
  table?: API.AiTablePayload;
  /** 执行步骤（step 事件累积，一轮对话一组，位于回复内容上方） */
  steps?: API.AgentStep[];
  pendingAction?: API.PendingAction;
  html?: string;
  actionStatus?: 'pending' | 'executing' | 'done' | 'error';
  actionResult?: string;
}

const panelVisible = ref(false);
const sessions = ref<API.AiSession[]>([]);
const currentSessionId = ref('');
const messages = ref<ChatMsg[]>([]);
const draft = ref('');
const sending = ref(false);
const selectVisible = ref(false);
const selectedPictures = ref<{ fileId: string; thumbnailUrl: string; fileUrl: string }[]>([]);
const messagesRef = ref<HTMLElement>();

// ==================== 窗口拖拽 / 缩放 ====================
const MIN_W = 320;
const MIN_H = 420;
const DEFAULT_W = 420;
const DEFAULT_H = 620;
const STORAGE_KEY = 'agentPanelLayout';
const panelSize = reactive({ width: DEFAULT_W, height: DEFAULT_H, x: 0, y: 0 });
const panelStyle = computed(() => ({
  width: `${panelSize.width}px`,
  height: `${panelSize.height}px`,
  left: `${panelSize.x}px`,
  top: `${panelSize.y}px`,
}));

// 是否已从缓存加载过布局（加载成功则不再用默认右下角位置覆盖）
let layoutLoaded = false;

// 从浏览器缓存读取面板尺寸/位置
function loadPanelLayout() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const data = JSON.parse(raw);
    if (
      data &&
      typeof data.width === 'number' &&
      typeof data.height === 'number' &&
      typeof data.x === 'number' &&
      typeof data.y === 'number'
    ) {
      panelSize.width = data.width;
      panelSize.height = data.height;
      panelSize.x = data.x;
      panelSize.y = data.y;
      layoutLoaded = true;
    }
  } catch (e) {
    // 缓存损坏时忽略，回退到默认布局
  }
}

// 将面板尺寸/位置写入浏览器缓存
function savePanelLayout() {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        width: panelSize.width,
        height: panelSize.height,
        x: panelSize.x,
        y: panelSize.y,
      })
    );
  } catch (e) {
    // 隐私模式 / 存储满等场景静默忽略
  }
}

const dragging = ref(false);
const resizing = ref(false);
let resizeDir: ResizeDir = 'se';
let startX = 0;
let startY = 0;
let origX = 0;
let origY = 0;
let origW = 0;
let origH = 0;

function clamp(v: number, min: number, max: number): number {
  return Math.min(Math.max(v, min), max);
}

function setIframesInteractable(interactable: boolean) {
  document.querySelectorAll('.agent-panel iframe').forEach((el) => {
    (el as HTMLElement).style.pointerEvents = interactable ? 'auto' : 'none';
  });
}

function startDrag(e: MouseEvent) {
  dragging.value = true;
  startX = e.clientX;
  startY = e.clientY;
  origX = panelSize.x;
  origY = panelSize.y;
  setIframesInteractable(false);
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
}

function onDrag(e: MouseEvent) {
  if (!dragging.value) return;
  panelSize.x = clamp(origX + e.clientX - startX, 0, Math.max(0, window.innerWidth - panelSize.width));
  panelSize.y = clamp(origY + e.clientY - startY, 0, Math.max(0, window.innerHeight - panelSize.height));
}

function stopDrag() {
  dragging.value = false;
  setIframesInteractable(true);
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
  savePanelLayout();
}

function startResize(e: MouseEvent, dir: ResizeDir) {
  resizing.value = true;
  resizeDir = dir;
  startX = e.clientX;
  startY = e.clientY;
  origX = panelSize.x;
  origY = panelSize.y;
  origW = panelSize.width;
  origH = panelSize.height;
  setIframesInteractable(false);
  document.addEventListener('mousemove', onResize);
  document.addEventListener('mouseup', stopResize);
}

function onResize(e: MouseEvent) {
  if (!resizing.value) return;
  const dx = e.clientX - startX;
  const dy = e.clientY - startY;
  let w = origW;
  let h = origH;
  let x = origX;
  let y = origY;

  // 向右：左边固定
  if (resizeDir.includes('e')) {
    w = clamp(origW + dx, MIN_W, Math.max(MIN_W, window.innerWidth - origX - 8));
  }
  // 向下：上边固定
  if (resizeDir.includes('s')) {
    h = clamp(origH + dy, MIN_H, Math.max(MIN_H, window.innerHeight - origY - 8));
  }
  // 向左：右边固定，同步修正 x，避免拖出视口后尺寸与位置不一致
  if (resizeDir.includes('w')) {
    const right = origX + origW;
    w = clamp(origW - dx, MIN_W, Math.max(MIN_W, right));
    x = right - w;
  }
  // 向上：下边固定，同步修正 y
  if (resizeDir.includes('n')) {
    const bottom = origY + origH;
    h = clamp(origH - dy, MIN_H, Math.max(MIN_H, bottom));
    y = bottom - h;
  }

  panelSize.width = w;
  panelSize.height = h;
  panelSize.x = x;
  panelSize.y = y;
}

function stopResize() {
  resizing.value = false;
  setIframesInteractable(true);
  document.removeEventListener('mousemove', onResize);
  document.removeEventListener('mouseup', stopResize);
  savePanelLayout();
}

// 复制消息文本（优先 Clipboard API，非安全上下文降级到 execCommand）
async function copyText(text?: string) {
  if (!text) return;
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.position = 'fixed';
      ta.style.opacity = '0';
      document.body.appendChild(ta);
      ta.select();
      const ok = document.execCommand('copy');
      document.body.removeChild(ta);
      if (!ok) throw new Error('execCommand copy failed');
    }
    ElMessage.success('已复制');
  } catch {
    ElMessage.error('复制失败，请手动选择文本');
  }
}

function initPanelPosition() {
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  // 视口变小后先收敛尺寸，避免恢复的布局超出屏幕
  panelSize.width = clamp(panelSize.width, MIN_W, Math.max(MIN_W, vw - 16));
  panelSize.height = clamp(panelSize.height, MIN_H, Math.max(MIN_H, vh - 16));
  if (layoutLoaded) {
    // 已从缓存恢复布局：仅做边界校正，防止窗口尺寸变化后跑出视口
    panelSize.x = clamp(panelSize.x, 0, Math.max(0, vw - panelSize.width));
    panelSize.y = clamp(panelSize.y, 0, Math.max(0, vh - panelSize.height));
    return;
  }
  // 首次使用：默认贴右下角
  panelSize.x = Math.max(0, vw - panelSize.width - 24);
  panelSize.y = Math.max(0, vh - panelSize.height - 24);
}

// 全屏预览照片
function previewUrls(urlList: string[], index: number) {
  if (!urlList || urlList.length === 0) return;
  $bus.emit('openImageViewer', { urlList, initialIndex: index });
}

function previewPhotos(photos: API.AiPhotoResult[], index: number) {
  const urlList = photos.map((p) => p.fileUrl || p.thumbnailUrl || '');
  // 携带照片 id，全局预览器据此提供下载、添加相册、照片详情（媒体数据）等能力
  const fileIdList = photos.map((p) => p.fileId).filter((id): id is string => !!id);
  $bus.emit('openImageViewer', { urlList, initialIndex: index, fileIdList });
}

let msgSeq = 0;
function genMsgId(): string {
  msgSeq += 1;
  return `local-${Date.now()}-${msgSeq}`;
}

/**
 * 当前轮次的步骤消息引用。
 *
 * <p>step 事件全部累积到这一条消息上，使其位于本轮回复内容上方；
 * 每轮发送前重置，避免跨轮串到同一条。
 */
let stepsMsg: ChatMsg | null = null;

function ensureStepsMsg(): ChatMsg {
  if (!stepsMsg) {
    stepsMsg = { id: genMsgId(), role: 'assistant', steps: [] };
    messages.value.push(stepsMsg);
  }
  return stepsMsg;
}

/** 解析 step 事件（data 为 {id, status, title?, detail?} JSON 字符串） */
function parseStep(raw?: string): API.AgentStep | undefined {
  if (!raw) return undefined;
  try {
    const o = JSON.parse(raw);
    if (!o || o.id === undefined || !o.status) return undefined;
    return { id: o.id, title: o.title, status: o.status, detail: o.detail };
  } catch {
    return undefined;
  }
}

/**
 * 流结束（done / error）时收敛仍在 running 的步骤。
 * SSE 异常中断时不会有终态事件，不处理会导致步骤条永远转圈。
 */
function finishRunningSteps(asFailed: boolean): void {
  if (!stepsMsg?.steps) return;
  for (const s of stepsMsg.steps) {
    if (s.status === 'running') {
      s.status = asFailed ? 'failed' : 'done';
      if (asFailed) s.detail = '已中断';
    }
  }
}

/** 把某步骤置为已完成（用户确认写操作后调用），步骤可能挂在另一条消息上 */
function markStepDone(stepId?: number, detail?: string): void {
  if (stepId == null) return;
  for (const m of messages.value) {
    const hit = m.steps?.find((s) => s.id === stepId);
    if (hit) {
      hit.status = 'done';
      hit.detail = detail || hit.detail || '已确认执行';
      return;
    }
  }
}

const canSend = computed(() => draft.value.trim().length > 0 && !sending.value);

function togglePanel() {
  panelVisible.value = !panelVisible.value;
  if (panelVisible.value) {
    initPanelPosition();
    scrollToBottom();
  }
}

function closePanel() {
  panelVisible.value = false;
}

function sessionLabel(s: API.AiSession): string {
  return s.title || '新会话';
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

// 加载会话列表
async function loadSessions() {
  try {
    const res = await listSessions();
    if (res.code === 200) {
      sessions.value = (res.data as API.AiSession[]) || [];
    }
  } catch {
    // 忽略会话列表加载失败
  }
}

// 初始化：有会话则选第一个，否则新建默认会话
async function initChat() {
  await loadSessions();
  if (sessions.value.length > 0) {
    await switchSession(sessions.value[0].sessionId);
  } else {
    await createCurrentSession();
  }
}

// 切换会话（加载历史消息）
async function switchSession(sessionId: string) {
  currentSessionId.value = sessionId;
  await loadMessages(sessionId);
}

async function handleSwitchSession(sessionId: string) {
  await switchSession(sessionId);
}

// 后端 extra 是 JSON 字符串（PendingActionVO 序列化），转前端 PendingAction 形状
function parsePendingAction(raw?: string): API.PendingAction | undefined {
  if (!raw) return undefined;
  try {
    const o = typeof raw === 'string' ? JSON.parse(raw) : raw;
    if (!o || !o.actionId) {
      console.warn('[AgentChat] 确认卡片数据缺少 actionId:', raw);
      return undefined;
    }
    return {
      actionId: o.actionId,
      type: o.actionType,
      title: o.title,
      params: { description: o.description },
      preview: o.preview || {},
      stepId: o.stepId,
    };
  } catch (e) {
    console.warn('[AgentChat] 确认卡片数据解析失败:', raw, e);
    return undefined;
  }
}

// extra(JSON字符串) → HTML 原文；兼容旧数据的裸 HTML 文本
function extractHtmlExtra(raw?: string): string {
  if (!raw) return '';
  try {
    const o = JSON.parse(raw);
    if (o && typeof o === 'object' && typeof o.html === 'string') {
      return o.html;
    }
  } catch {
    // 非 JSON → 视为旧数据中的 HTML 原文
  }
  return raw;
}

// 从后端 AiMessage[] 转换为前端 ChatMsg[]
function toChatMsg(m: API.AiMessage): ChatMsg {
  const base: ChatMsg = {
    id: genMsgId(),
    role: m.role === 'user' ? 'user' : 'assistant',
  };
  if (m.role === 'user') {
    base.content = m.content;
    return base;
  }
  // assistant 消息按 contentType 分发（后端 contentType: text / html / pending_action）
  switch (m.contentType) {
    case 'pending_action':
      base.pendingAction = parsePendingAction(m.extra);
      base.actionStatus = 'done'; // 历史记录不可再确认
      break;
    case 'photos':
      base.content = m.content;
      base.photos = parsePhotos(m.extra);
      break;
    case 'download':
      base.content = m.content;
      base.downloads = parseDownloads(m.extra);
      break;
    case 'metric':
      base.content = m.content;
      base.metric = parseMetric(m.extra);
      break;
    case 'table':
      base.content = m.content;
      base.table = parseTablePayload(m.extra);
      break;
    case 'steps':
      base.content = m.content;
      base.steps = parseSteps(m.extra);
      break;
    case 'html':
      base.content = '已生成网页';
      // extra 兼容两种格式：JSON 对象 {"html":"..."}（新）与 HTML 原文（历史数据）
      base.html = extractHtmlExtra(m.extra) || m.content;
      break;
    default:
      base.content = m.content;
  }
  return base;
}

async function loadMessages(sessionId: string) {
  messages.value = [];
  try {
    const res = await listMessages(sessionId);
    if (res.code === 200) {
      const list = (res.data as API.AiMessage[]) || [];
      messages.value = list.map(toChatMsg);
      scrollToBottom();
    }
  } catch {
    // 忽略历史加载失败
  }
}

// 新建会话
async function handleNewSession() {
  await createCurrentSession();
}

// 删除当前会话（弹窗二次确认）
async function handleDeleteSession() {
  const sessionId = currentSessionId.value;
  if (!sessionId || sending.value) return;
  const target = sessions.value.find((s) => s.sessionId === sessionId);
  try {
    await ElMessageBox.confirm(
      `确定删除会话「${target?.title || '新会话'}」吗？删除后不可恢复。`,
      '删除会话',
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
    const res = await deleteSession(sessionId);
    if (res.code === 200) {
      sessions.value = sessions.value.filter((s) => s.sessionId !== sessionId);
      if (currentSessionId.value === sessionId) {
        currentSessionId.value = '';
        messages.value = [];
        // 自动切换到剩余会话；无剩余会话则新建
        if (sessions.value.length > 0) {
          await switchSession(sessions.value[0].sessionId);
        } else {
          await createCurrentSession();
        }
      }
      ElMessage.success('会话已删除');
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败');
  }
}

async function createCurrentSession() {
  try {
    // agent 类型已统一由主 Agent 判别，固定传 mixed（仅为兼容后端字段）
    const res = await createSession({ agentType: 'mixed' });
    if (res.code === 200) {
      const data = res.data as API.AiSession;
      currentSessionId.value = data.sessionId;
      messages.value = [];
      sessions.value.unshift(data);
      draft.value = '';
      selectedPictures.value = [];
      scrollToBottom();
    }
  } catch {
    // 新建失败时仍允许本地输入（无 sessionId，后端兜底）
    currentSessionId.value = '';
    messages.value = [];
  }
}

// （Agent 类型切换已移除：所有任务统一由主 Agent 判别路由）

// 发送消息
async function send() {
  const content = draft.value.trim();
  if (!content || sending.value) return;

  // 无会话时先创建
  if (!currentSessionId.value) {
    await createCurrentSession();
  }

  const fileIds = selectedPictures.value.map((p) => p.fileId);
  const thumbnails = selectedPictures.value.map((p) => p.thumbnailUrl);
  const thumbUrls = selectedPictures.value.map((p) => p.fileUrl || p.thumbnailUrl);

  messages.value.push({
    id: genMsgId(),
    role: 'user',
    content,
    thumbnails,
    thumbUrls,
  });
  draft.value = '';
  selectedPictures.value = [];
  // 新的一轮：重置步骤消息引用，避免本轮步骤串到上一轮那一条上
  stepsMsg = null;
  sending.value = true;
  scrollToBottom();

  try {
    await streamChat(
      {
        sessionId: currentSessionId.value || undefined,
        content,
        fileIds,
      },
      handleChatEvent,
    );
    // 首条消息后刷新会话列表（等标题生成）
    if (messages.value.length <= 2) {
      loadSessions();
    }
  } catch (e: any) {
    pushError(e?.message || '请求失败，请稍后重试');
  } finally {
    sending.value = false;
    scrollToBottom();
  }
}

// 后端 photos 事件 data 是 FileInfo 数组 JSON 字符串
function parsePhotos(raw?: string): API.AiPhotoResult[] | undefined {
  if (!raw) return undefined;
  try {
    const arr = JSON.parse(raw);
    return Array.isArray(arr) ? (arr as API.AiPhotoResult[]) : undefined;
  } catch {
    return undefined;
  }
}

// 后端 download 事件 data 是 {fileName, fileId}[] JSON 字符串
function parseDownloads(raw?: string): API.AiDownloadItem[] | undefined {
  if (!raw) return undefined;
  try {
    const arr = JSON.parse(raw);
    if (!Array.isArray(arr)) return undefined;
    return (arr as API.AiDownloadItem[]).filter(
      (it) => it && typeof it.fileId === 'string' && it.fileId,
    );
  } catch {
    return undefined;
  }
}

// 后端 metric 事件 data 是 {label, value} JSON 字符串
function parseMetric(raw?: string): API.AiMetricPayload | undefined {
  if (!raw) return undefined;
  try {
    const o = JSON.parse(raw);
    if (!o || typeof o !== 'object' || o.value === undefined) return undefined;
    return { label: o.label ?? '查询结果', value: o.value };
  } catch {
    return undefined;
  }
}

// 后端 table 事件 data 是 {columns:[{key,label,type}], rows:[...]} JSON 字符串
function parseTablePayload(raw?: string): API.AiTablePayload | undefined {
  if (!raw) return undefined;
  try {
    const o = JSON.parse(raw);
    if (!o || typeof o !== 'object') return undefined;
    // 列定义缺失时按首行键名兜底，保证表格仍能渲染
    const columns: API.AiTableColumn[] =
      Array.isArray(o.columns) && o.columns.length
        ? o.columns
        : Object.keys(o.rows?.[0] ?? {}).map((k) => ({ key: k, label: k, type: 'text' }));
    const rows = Array.isArray(o.rows) ? (o.rows as Record<string, any>[]) : [];
    return { columns, rows };
  } catch {
    return undefined;
  }
}

/**
 * 历史步骤：extra 存的是 step 事件数组 [{type:'step', data:{...}}]。
 * 同一 id 的 running 与终态需合并为一条，否则历史里会出现重复步骤。
 */
function parseSteps(raw?: string): API.AgentStep[] | undefined {
  if (!raw) return undefined;
  try {
    const arr = JSON.parse(raw);
    if (!Array.isArray(arr)) return undefined;
    const steps: API.AgentStep[] = [];
    for (const e of arr) {
      const d = e?.data;
      if (!d || typeof d !== 'object' || d.id === undefined) continue;
      const item: API.AgentStep = {
        id: d.id,
        title: d.title,
        status: d.status,
        detail: d.detail,
      };
      const i = steps.findIndex((s) => s.id === d.id);
      if (i >= 0) {
        steps[i] = { ...steps[i], ...item };
      } else {
        steps.push(item);
      }
    }
    return steps.length ? steps : undefined;
  } catch {
    return undefined;
  }
}

// 处理后端 SSE 事件（text / card / photos / html / tool / done / error）
function handleChatEvent(ev: AiChatEvent) {
  switch (ev.type) {
    case 'text': {
      if (!ev.data) break;
      messages.value.push({
        id: genMsgId(),
        role: 'assistant',
        content: ev.data,
      });
      break;
    }
    case 'card': {
      const pendingAction = parsePendingAction(ev.data);
      if (pendingAction) {
        messages.value.push({
          id: genMsgId(),
          role: 'assistant',
          pendingAction,
          actionStatus: 'pending',
        });
      }
      break;
    }
    case 'html': {
      messages.value.push({
        id: genMsgId(),
        role: 'assistant',
        content: '已生成网页',
        html: ev.data,
      });
      break;
    }
    case 'photos': {
      const photos = parsePhotos(ev.data);
      if (photos && photos.length) {
        messages.value.push({
          id: genMsgId(),
          role: 'assistant',
          photos,
        });
      }
      break;
    }
    case 'download': {
      const downloads = parseDownloads(ev.data);
      if (downloads && downloads.length) {
        messages.value.push({
          id: genMsgId(),
          role: 'assistant',
          downloads,
        });
      }
      break;
    }
    case 'metric': {
      // 问数标量结果：data 为 {label, value} JSON 字符串
      const metric = parseMetric(ev.data);
      if (metric) {
        messages.value.push({ id: genMsgId(), role: 'assistant', metric });
      }
      break;
    }
    case 'table': {
      // 问数明细表格：data 为 {columns, rows} JSON 字符串
      const table = parseTablePayload(ev.data);
      if (table && table.rows?.length) {
        messages.value.push({ id: genMsgId(), role: 'assistant', table });
      }
      break;
    }
    case 'step': {
      // 执行步骤：同一 id 会先推 running 再推终态，按 id 合并更新
      const step = parseStep(ev.data);
      if (step) {
        const list = ensureStepsMsg().steps!;
        const idx = list.findIndex((s) => s.id === step.id);
        if (idx >= 0) {
          list[idx] = { ...list[idx], ...step };
        } else {
          list.push(step);
        }
      }
      break;
    }
    case 'tool': {
      // 主 Agent 工具调用进度：data 为 {name, arguments} JSON 字符串
      try {
        const info = JSON.parse(ev.data) as { name?: string };
        if (info?.name) {
          messages.value.push({
            id: genMsgId(),
            role: 'assistant',
            content: `正在执行子任务：${info.name}`,
          });
        }
      } catch {
        // 忽略格式异常的进度事件
      }
      break;
    }
    case 'title': {
      // 后端已根据对话内容生成会话标题，即时更新会话列表展示
      const title = (ev.data || '').trim();
      if (title) {
        const target = sessions.value.find((s) => s.sessionId === currentSessionId.value);
        if (target) {
          target.title = title;
        }
      }
      break;
    }
    case 'error': {
      // 流异常中断：收敛仍在 running 的步骤，避免前端永远转圈
      finishRunningSteps(true);
      pushError(ev.data || '请求失败');
      break;
    }
    case 'done':
      finishRunningSteps(false);
      break;
    default:
      break;
  }
  scrollToBottom();
}

function pushError(msg: string) {
  messages.value.push({
    id: genMsgId(),
    role: 'assistant',
    content: msg,
  });
  ElMessage.error(msg);
}

// 确认执行写操作
async function confirmAction(msg: ChatMsg) {
  if (!msg.pendingAction) return;
  msg.actionStatus = 'executing';
  try {
    const res = await agentExecute({ actionId: msg.pendingAction.actionId });
    if (res.code === 200) {
      msg.actionStatus = 'done';
      const data = res.data as { message?: string };
      msg.actionResult = data?.message || '已完成';
      // 步骤条上对应的那一步从「待确认」转为「已完成」
      markStepDone(msg.pendingAction.stepId, msg.actionResult);
      ElMessage.success(msg.actionResult);
    } else {
      msg.actionStatus = 'error';
      ElMessage.error(res.message || '执行失败');
    }
  } catch (e: any) {
    msg.actionStatus = 'error';
    // 透出后端具体原因（如"操作已过期，请重新发起"）
    ElMessage.error(e?.response?.data?.message || e?.message || '执行失败');
  }
}

function cancelAction(msg: ChatMsg) {
  msg.actionStatus = 'error';
  msg.actionResult = '已取消';
}

// 服务端打包下载：POST /ai/download/zip → ZIP blob → 保存；
// 服务端从对象存储拉流打包，无跨域/签名过期问题，zip 内重名自动编号
async function downloadFiles(msg: ChatMsg) {
  if (!msg.downloads?.length || msg.downloading) return;
  msg.downloading = true;
  try {
    const token = localStorage.getItem('token');
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) headers.Authorization = token;
    const resp = await fetch(`${requestPublicConfig.baseUrl}/ai/download/zip`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ fileIds: msg.downloads.map((d) => d.fileId) }),
    });
    if (!resp.ok) {
      // 后端业务异常时响应为 JSON，尝试透出错误信息
      let msgText = `HTTP ${resp.status}`;
      try {
        const err = await resp.json();
        if (err?.message) msgText = err.message;
      } catch {
        // 非 JSON 响应忽略
      }
      throw new Error(msgText);
    }
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `照片_${msg.downloads.length}张.zip`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('压缩包已开始下载');
  } catch (e: any) {
    ElMessage.error(e?.message || '打包下载失败');
  } finally {
    msg.downloading = false;
  }
}

// 下载 HTML
function downloadHtml(msg: ChatMsg) {
  if (!msg.html) return;
  const blob = new Blob([msg.html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'ai-page.html';
  a.click();
  URL.revokeObjectURL(url);
}

// 新窗口打开 HTML
function openHtml(msg: ChatMsg) {
  if (!msg.html) return;
  const blob = new Blob([msg.html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank');
}

// 照片上下文
function openSelectPicture() {
  selectVisible.value = true;
}

function removePicture(fileId: string) {
  selectedPictures.value = selectedPictures.value.filter((p) => p.fileId !== fileId);
}

function clearPictures() {
  selectedPictures.value = [];
}

function onSelectPicture(list: any) {
  const pics = (list as API.SelectPicture[]) || [];
  selectedPictures.value = pics.map((p) => ({
    fileId: p.fileId,
    thumbnailUrl: p.thumbnailUrl,
    fileUrl: p.fileUrl || p.thumbnailUrl,
  }));
  selectVisible.value = false;
}

onMounted(() => {
  $bus.off('agentSelectPicture');
  $bus.on('agentSelectPicture', onSelectPicture);
  loadPanelLayout();
  initChat();
});

onUnmounted(() => {
  $bus.off('agentSelectPicture');
  stopDrag();
  stopResize();
});
</script>

<style scoped>
.agent-chat {
  position: relative;
  /* 低于照片预览器(1800)与弹窗(2000+)，保证 AI 预览照片时不被面板遮挡 */
  z-index: 1500;
}

/* 浮动按钮 */
.agent-fab {
  position: fixed;
  right: 24px;
  bottom: 88px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3d7eff, #6b8cff);
  box-shadow: 0 6px 18px rgba(61, 126, 255, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.25s ease;
}

.agent-fab:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(61, 126, 255, 0.55);
}

.agent-fab.active {
  background: #fff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
}

.agent-fab.active .fab-icon {
  color: #666;
}

.fab-icon {
  font-size: 26px;
  color: #fff;
}

/* 面板 */
.agent-panel {
  position: fixed;
  top: 0;
  left: 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agent-panel.is-interacting {
  user-select: none;
}

/* 消息区允许鼠标选中复制（覆盖 #basicLayout 布局级的 user-select: none） */
.agent-messages {
  user-select: text;
}

/* 拖拽/缩放面板期间临时禁用选中，避免拖动时误选文字 */
.agent-panel.is-interacting .agent-messages {
  user-select: none;
}

.agent-slide-enter-active,
.agent-slide-leave-active {
  transition: all 0.25s ease;
}

.agent-slide-enter-from,
.agent-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

/* 顶栏 */
.agent-panel-header {
  height: 48px;
  padding: 0 12px 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #3d7eff, #6b8cff);
  color: #fff;
  cursor: move;
  user-select: none;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}

.title-icon {
  font-size: 20px;
}

.panel-ops {
  display: flex;
  gap: 2px;
}

.panel-ops .el-button {
  color: #fff;
}

.panel-ops .el-button:hover {
  color: #e0eaff;
}

.op-icon {
  font-size: 18px;
}

/* 工具栏 */
.agent-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.session-select {
  flex: 1;
  min-width: 0;
}

/* 删除会话按钮 hover 提示危险操作 */
.session-delete-btn:not(.is-disabled):hover {
  color: #f56c6c;
}

/* 下载卡片 */
.download-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 6px 0;
  padding: 10px 14px;
  max-width: 88%;
  border: 1px solid #d6e4ff;
  border-radius: 10px;
  background: #f0f7ff;
}

.download-info {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.download-icon {
  font-size: 22px;
  color: #3d7eff;
  flex-shrink: 0;
}

.download-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.download-title {
  font-size: 13px;
  font-weight: 600;
  color: #2b3a55;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.download-sub {
  font-size: 12px;
  color: #8a94a6;
}

/* 问数指标 */
.metric-card {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 6px 0;
  padding: 10px 16px;
  border: 1px solid #d6e4ff;
  border-radius: 10px;
  background: #f0f7ff;
}

.metric-value {
  font-size: 26px;
  font-weight: 700;
  color: #3d7eff;
  line-height: 1;
}

.metric-label {
  font-size: 12px;
  color: #6b7787;
}

/* 问数表格（宽度超出面板时横向滚动，不撑破布局） */
.result-table {
  margin: 6px 0;
  max-width: 100%;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: auto;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.result-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.result-table th {
  padding: 8px 10px;
  text-align: left;
  color: #6b7787;
  font-weight: 600;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  white-space: nowrap;
}

.result-table td {
  padding: 8px 10px;
  color: #2b3a55;
  border-bottom: 1px solid #f5f5f5;
  vertical-align: middle;
}

.result-table tr:last-child td {
  border-bottom: none;
}

.table-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
  cursor: pointer;
}

/* 消息列表 */
.agent-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agent-empty {
  margin: auto;
  text-align: center;
  color: #999;
}

.empty-icon {
  font-size: 40px;
  color: #c0c4cc;
  margin-bottom: 8px;
}

.agent-empty p {
  margin: 4px 0;
  font-size: 14px;
}

.empty-tip {
  font-size: 12px;
  color: #bbb;
}

.agent-msg {
  display: flex;
  flex-direction: column;
  max-width: 86%;
}

.agent-msg.is-user {
  align-self: flex-end;
  align-items: flex-end;
}

.agent-msg.is-ai {
  align-self: flex-start;
  align-items: flex-start;
}

/* 气泡 + 复制按钮：用户消息按钮在左，AI 消息按钮在右 */
.bubble-wrap {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
}

.agent-msg.is-user .bubble-wrap {
  flex-direction: row-reverse;
}

.bubble-wrap .bubble {
  min-width: 0;
}

.copy-btn {
  flex: none;
  width: 22px;
  height: 22px;
  margin-top: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0b3b8;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.18s ease, color 0.18s ease, background 0.18s ease;
}

/* 触屏设备无 hover，直接常显 */
.agent-msg:hover .copy-btn {
  opacity: 1;
}

@media (hover: none) {
  .copy-btn {
    opacity: 1;
  }
}

.copy-btn:hover {
  color: #3d7eff;
  background: rgba(61, 126, 255, 0.1);
}

.copy-btn:active {
  transform: scale(0.9);
}

.copy-icon {
  font-size: 14px;
}

.bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.user-bubble {
  background: #3d7eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.msg-thumbs {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.msg-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
}

/* 思考中 */
.thinking {
  display: flex;
  gap: 4px;
  padding: 12px 14px;
}

.thinking .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #a0b4ff;
  animation: blink 1.2s infinite ease-in-out;
}

.thinking .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%, 80%, 100% {
    opacity: 0.2;
  }
  40% {
    opacity: 1;
  }
}

/* 照片网格 */
.msg-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.msg-photo {
  width: 88px;
  height: 88px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

/* 确认卡片 */
.pending-card {
  margin-top: 8px;
  background: #fff;
  border: 1px solid #ffd591;
  border-radius: 10px;
  padding: 10px 12px;
  width: 260px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.pending-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #b26a00;
}

.pending-icon {
  color: #faad14;
}

.pending-preview {
  margin-top: 8px;
}

.pending-thumbs {
  display: flex;
  gap: 4px;
  align-items: center;
}

.pending-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
}

.pending-more {
  font-size: 12px;
  color: #999;
}

.pending-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #666;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pending-danger {
  color: #f56c6c;
  font-weight: 500;
}

.pending-actions {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
}

/* HTML 预览 */
.html-preview {
  margin-top: 8px;
  width: 320px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.html-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-bottom: 1px solid #f0f0f0;
}

.html-label {
  font-size: 13px;
  color: #666;
}

.html-ops {
  display: flex;
  gap: 4px;
}

.html-frame {
  width: 100%;
  height: 220px;
  border: none;
  display: block;
}

/* 照片上下文条 */
.agent-context {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.context-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

.context-thumbs {
  display: flex;
  gap: 6px;
  flex: 1;
  overflow-x: auto;
}

.context-item {
  position: relative;
  flex: none;
}

.context-thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
  cursor: pointer;
}

.context-remove {
  position: absolute;
  top: -5px;
  right: -5px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 输入区 */
.agent-input {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  padding: 10px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fff;
}

.input-pic-btn {
  flex: none;
}

.input-textarea {
  flex: 1;
}

.input-textarea :deep(.el-textarea__inner) {
  border-radius: 10px;
  background: #f5f7fa;
  box-shadow: none;
  padding: 8px 12px;
}

.send-btn {
  flex: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  padding: 0;
}

/* 四边 + 四角缩放手柄 */
.resize-handle {
  position: absolute;
  z-index: 10;
}

/* 四条边：细条热区，内缩避让四角手柄 */
.resize-n,
.resize-s {
  left: 18px;
  right: 18px;
  height: 6px;
  cursor: ns-resize;
}

.resize-n {
  top: 0;
}

.resize-s {
  bottom: 0;
}

.resize-e,
.resize-w {
  top: 18px;
  bottom: 18px;
  width: 6px;
  cursor: ew-resize;
}

.resize-e {
  right: 0;
}

.resize-w {
  left: 0;
}

/* 四个角 */
.resize-nw,
.resize-ne,
.resize-sw,
.resize-se {
  width: 18px;
  height: 18px;
}

.resize-nw {
  top: 0;
  left: 0;
  cursor: nwse-resize;
}

.resize-ne {
  top: 0;
  right: 0;
  cursor: nesw-resize;
}

.resize-sw {
  bottom: 0;
  left: 0;
  cursor: nesw-resize;
}

.resize-se {
  right: 0;
  bottom: 0;
  cursor: nwse-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b0b3b8;
  border-bottom-right-radius: 16px;
}

.resize-se:hover {
  color: #3d7eff;
}

.resize-icon {
  font-size: 14px;
  pointer-events: none;
}

/* 选照片弹窗 */
.select-wrap {
  height: 65vh;
  overflow: hidden;
}
</style>

<style>
.agent-select-dialog {
  height: 80vh;
}

.agent-select-dialog .el-dialog__body {
  height: calc(100% - 55px);
  padding: 0;
  overflow: hidden;
}
</style>
