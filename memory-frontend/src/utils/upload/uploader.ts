import { reactive } from 'vue';
import $bus from '@/utils/bus';
import {
  preCheckUpload,
  uploadChunk,
  completeUpload,
  abortUpload,
  resolveErrorMessage,
  isRetryableError,
  extractErrorCode,
  extractBackendMessage,
} from '@/api/file/upload';
import type { HashWorkerRequest, HashWorkerResponse } from './hash.worker';

/** 与后端 FileUtil 白名单保持一致 */
export const ACCEPT_TYPES = ['.png', '.jpg', '.jpeg', '.gif', '.bmp', '.mp4', '.rmvb', '.mkv', '.wmv', '.flv'];

/** 哈希阶段的切片大小，与上传分片大小无关 */
const HASH_CHUNK_SIZE = 2 * 1024 * 1024;

/** 单文件内并发上传的分片数 */
const CHUNK_CONCURRENCY = 3;

/** 同时处理的文件数 */
const FILE_CONCURRENCY = 2;

/** 单个分片最大重试次数 */
const MAX_CHUNK_RETRY = 3;

/** 断点续传记录有效期（毫秒），与后端 upload.task-ttl-hours 对齐 */
const PERSIST_TTL = 24 * 60 * 60 * 1000;

const PERSIST_KEY = 'memory:upload:progress';

export type UploadStatus =
  | 'waiting'
  | 'hashing'
  | 'uploading'
  | 'paused'
  | 'merging'
  | 'success'
  | 'error'
  | 'canceled';

export interface UploadItem {
  id: string;
  file: File;
  name: string;
  size: number;
  albumId: number;
  status: UploadStatus;
  /** 总体进度 0~100 */
  progress: number;
  /** 哈希阶段进度 0~1 */
  hashProgress: number;
  uploadedBytes: number;
  /** 实时速度（字节/秒） */
  speed: number;
  md5: string;
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  uploadedChunks: Set<number>;
  /** 正在传输中的分片已传字节数，用于平滑进度 */
  inflightBytes: Map<number, number>;
  errorMsg: string;
  instant: boolean;
  fileId: string;
  category: 'image' | 'video' | 'other';
  /** 列表缩略图：图片为 blob URL，视频为抓帧 dataURL */
  thumbnail: string;
}

interface PersistedRecord {
  md5: string;
  uploadId: string;
  albumId: number;
  ts: number;
}

type PersistedMap = Record<string, PersistedRecord>;

// ---------------------------------------------------------------- 状态

export const uploadState = reactive<{ list: UploadItem[] }>({ list: [] });

const controllers = new Map<string, AbortController>();
const speedSnapshot = new Map<string, { bytes: number; time: number }>();
let speedTimer: number | null = null;
let flushTimer: number | null = null;
const pendingAlbumIds = new Set<number>();
let sequence = 0;

// ---------------------------------------------------------------- 工具

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 读取上传状态。
 *
 * 状态会被暂停/取消等操作在异步流程之外修改，这里以函数返回值的方式读取，
 * 避免 TS 的控制流窄化把"已取消/已暂停"这类有效判断误判为永不成立。
 */
function statusOf(item: UploadItem): UploadStatus {
  return item.status;
}

/** 暂停时挂起，直到恢复或取消 */
async function waitIfPaused(item: UploadItem): Promise<void> {
  while (statusOf(item) === 'paused') {
    await sleep(300);
    if (statusOf(item) === 'canceled') return;
  }
}

function fileKey(file: File): string {
  return `${file.name}::${file.size}::${file.lastModified}`;
}

function chunkBytes(item: UploadItem, index: number): number {
  const start = index * item.chunkSize;
  return Math.max(0, Math.min(item.chunkSize, item.size - start));
}

function detectCategory(name: string): UploadItem['category'] {
  const ext = name.slice(name.lastIndexOf('.')).toLowerCase();
  if (['.png', '.jpg', '.jpeg', '.gif', '.bmp'].includes(ext)) return 'image';
  if (['.mp4', '.rmvb', '.mkv', '.wmv', '.flv'].includes(ext)) return 'video';
  return 'other';
}

/** 抓取视频首帧，返回 JPEG dataURL；失败返回空串 */
function captureVideoFrame(file: File): Promise<string> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file);
    const video = document.createElement('video');
    video.muted = true;
    video.playsInline = true;
    video.preload = 'auto';
    video.src = url;

    let settled = false;
    const finish = (dataUrl: string) => {
      if (settled) return;
      settled = true;
      URL.revokeObjectURL(url);
      resolve(dataUrl);
    };

    const draw = () => {
      const canvas = document.createElement('canvas');
      const width = video.videoWidth || 320;
      const height = video.videoHeight || 180;
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      let dataUrl = '';
      if (ctx) {
        try {
          ctx.drawImage(video, 0, 0, width, height);
          dataUrl = canvas.toDataURL('image/jpeg', 0.6);
        } catch {
          dataUrl = '';
        }
      }
      finish(dataUrl);
    };

    video.onloadeddata = () => {
      // 0 时刻常是黑帧，跳到 0.1s 附近再抓；seeked 未触发时超时兜底直接抓当前帧
      window.setTimeout(() => {
        if (!settled) draw();
      }, 800);
      const target = Number.isFinite(video.duration) && video.duration > 0
        ? Math.min(0.1, video.duration)
        : 0.1;
      video.currentTime = target;
    };
    video.onseeked = draw;
    video.onerror = () => finish('');
  });
}

function generateThumbnail(item: UploadItem): void {
  if (item.category === 'image') {
    item.thumbnail = URL.createObjectURL(item.file);
  } else if (item.category === 'video') {
    void captureVideoFrame(item.file).then((dataUrl) => {
      // 抓帧期间项可能已被移除，此时丢弃结果
      if (uploadState.list.includes(item)) {
        item.thumbnail = dataUrl;
      }
    });
  }
}

/** 释放缩略图占用的 blob URL，项从列表移除前调用 */
function disposeThumbnail(item: UploadItem): void {
  if (item.thumbnail.startsWith('blob:')) {
    URL.revokeObjectURL(item.thumbnail);
  }
  item.thumbnail = '';
}

function readPersisted(): PersistedMap {
  try {
    const raw = localStorage.getItem(PERSIST_KEY);
    return raw ? (JSON.parse(raw) as PersistedMap) : {};
  } catch {
    return {};
  }
}

function writePersisted(map: PersistedMap) {
  try {
    localStorage.setItem(PERSIST_KEY, JSON.stringify(map));
  } catch {
    // 存储不可用不影响上传
  }
}

function persistProgress(item: UploadItem) {
  if (!item.uploadId || !item.md5) return;
  const map = readPersisted();
  map[fileKey(item.file)] = {
    md5: item.md5,
    uploadId: item.uploadId,
    albumId: item.albumId,
    ts: Date.now(),
  };
  writePersisted(map);
}

function clearPersisted(item: UploadItem) {
  const map = readPersisted();
  if (map[fileKey(item.file)]) {
    delete map[fileKey(item.file)];
    writePersisted(map);
  }
}

/** 清理超过有效期的断点记录 */
export function pruneExpiredProgress() {
  const map = readPersisted();
  const now = Date.now();
  let changed = false;
  Object.keys(map).forEach((key) => {
    if (now - map[key].ts > PERSIST_TTL) {
      delete map[key];
      changed = true;
    }
  });
  if (changed) {
    writePersisted(map);
  }
}

function updateProgress(item: UploadItem) {
  let bytes = 0;
  item.uploadedChunks.forEach((index) => {
    bytes += chunkBytes(item, index);
  });
  item.inflightBytes.forEach((loaded) => {
    bytes += loaded;
  });
  item.uploadedBytes = Math.min(bytes, item.size);

  if (item.status === 'hashing') {
    // 哈希阶段折算到 0~10%，避免大文件长时间显示 0%
    item.progress = Math.min(10, item.hashProgress * 10);
  } else if (item.size > 0) {
    item.progress = Math.min(100, (item.uploadedBytes / item.size) * 100);
  }
}

function startSpeedTicker() {
  if (speedTimer !== null) return;
  speedTimer = window.setInterval(() => {
    const now = Date.now();
    uploadState.list.forEach((item) => {
      const snapshot = speedSnapshot.get(item.id);
      if (snapshot) {
        const seconds = (now - snapshot.time) / 1000;
        item.speed = seconds > 0 ? Math.max(0, (item.uploadedBytes - snapshot.bytes) / seconds) : 0;
      }
      speedSnapshot.set(item.id, { bytes: item.uploadedBytes, time: now });
    });
  }, 1000);
}

function stopSpeedTickerIfIdle() {
  const active = uploadState.list.some(
    (item) => item.status === 'uploading' || item.status === 'hashing' || item.status === 'merging',
  );
  if (!active && speedTimer !== null) {
    window.clearInterval(speedTimer);
    speedTimer = null;
    speedSnapshot.clear();
  }
}

/** 合并短时间内的多次成功通知，避免批量上传时列表被反复刷新 */
function notifyUploadSuccess(albumId: number) {
  pendingAlbumIds.add(albumId);
  if (flushTimer !== null) return;
  flushTimer = window.setTimeout(() => {
    flushTimer = null;
    const albumIds = Array.from(pendingAlbumIds);
    pendingAlbumIds.clear();
    albumIds.forEach((id) => $bus.emit('uploadSucess', id));
  }, 800);
}

// ---------------------------------------------------------------- 哈希

function computeMd5(item: UploadItem): Promise<string> {
  return new Promise((resolve, reject) => {
    let worker: Worker;
    try {
      worker = new Worker(new URL('./hash.worker.ts', import.meta.url), { type: 'module' });
    } catch (error) {
      reject(new Error('浏览器不支持 Web Worker，无法计算文件指纹'));
      return;
    }

    worker.onmessage = (event: MessageEvent<HashWorkerResponse>) => {
      const message = event.data;
      if (message.type === 'progress') {
        item.hashProgress = message.progress;
        updateProgress(item);
      } else if (message.type === 'done') {
        worker.terminate();
        resolve(message.md5);
      } else {
        worker.terminate();
        reject(new Error(message.message));
      }
    };

    worker.onerror = (event) => {
      worker.terminate();
      reject(new Error(event.message || '文件指纹计算失败'));
    };

    const request: HashWorkerRequest = { file: item.file, chunkSize: HASH_CHUNK_SIZE };
    worker.postMessage(request);
  });
}

// ---------------------------------------------------------------- 分片上传

async function uploadChunkWithRetry(item: UploadItem, index: number): Promise<void> {
  const start = index * item.chunkSize;
  const end = Math.min(start + item.chunkSize, item.size);
  const blob = item.file.slice(start, end);
  const signal = controllers.get(item.id)?.signal;

  let lastError: unknown;

  for (let attempt = 1; attempt <= MAX_CHUNK_RETRY; attempt += 1) {
    if (item.status === 'canceled') return;

    // 暂停时挂起，直到恢复或取消
    while (statusOf(item) === 'paused') {
      await sleep(300);
      if (statusOf(item) === 'canceled') return;
    }

    try {
      const response = await uploadChunk(
        { uploadId: item.uploadId, chunkIndex: index, chunk: blob },
        (loaded) => {
          item.inflightBytes.set(index, Math.min(loaded, blob.size));
          updateProgress(item);
        },
        signal,
      );

      if (response.data.code !== 200) {
        const error = new Error(resolveErrorMessage(response.data.code, response.data.message));
        (error as Error & { code?: number }).code = response.data.code;
        throw error;
      }

      item.uploadedChunks.add(index);
      item.inflightBytes.delete(index);
      updateProgress(item);
      persistProgress(item);
      return;
    } catch (error) {
      item.inflightBytes.delete(index);
      lastError = error;

      if (statusOf(item) === 'canceled') return;

      const code = extractErrorCode(error);
      const hasResponse = Boolean((error as { response?: unknown })?.response);
      const retryable = isRetryableError(code, hasResponse) || !hasResponse;

      if (!retryable || attempt === MAX_CHUNK_RETRY) {
        throw error;
      }
      // 指数退避，避免服务端抖动时雪崩
      await sleep(500 * 2 ** (attempt - 1));
    }
  }

  throw lastError;
}

async function uploadAllChunks(item: UploadItem): Promise<void> {
  const pending: number[] = [];
  for (let i = 0; i < item.totalChunks; i += 1) {
    if (!item.uploadedChunks.has(i)) {
      pending.push(i);
    }
  }
  if (pending.length === 0) return;

  let cursor = 0;
  const workerCount = Math.min(CHUNK_CONCURRENCY, pending.length);

  const worker = async () => {
    while (true) {
      if (item.status === 'canceled') return;
      const index = cursor;
      cursor += 1;
      if (index >= pending.length) return;
      await uploadChunkWithRetry(item, pending[index]);
    }
  };

  await Promise.all(Array.from({ length: workerCount }, () => worker()));
}

// ---------------------------------------------------------------- 主流程

async function processItem(item: UploadItem): Promise<void> {
  try {
    // 1. 计算文件指纹（命中本地断点记录时可跳过）
    if (!item.md5) {
      item.status = 'hashing';
      startSpeedTicker();
      item.md5 = await computeMd5(item);
    }
    if (item.status === 'canceled') return;
    // 哈希或排队期间被暂停时，在此等待恢复
    await waitIfPaused(item);
    if (statusOf(item) === 'canceled') return;

    // 2. 预检：秒传 / 创建任务 / 获取续传进度
    const preCheckResponse = await preCheckUpload({
      filename: item.name,
      size: item.size,
      md5: item.md5,
      lastModified: String(item.file.lastModified),
      albumId: item.albumId,
      uploadId: item.uploadId || undefined,
    });
    const preCheck = preCheckResponse.data;
    if (preCheck.code !== 200) {
      throw new Error(resolveErrorMessage(preCheck.code, preCheck.message));
    }

    if (preCheck.data.uploaded) {
      // 秒传命中
      item.instant = true;
      item.fileId = preCheck.data.fileId;
      item.progress = 100;
      item.uploadedBytes = item.size;
      item.status = 'success';
      clearPersisted(item);
      notifyUploadSuccess(item.albumId);
      return;
    }

    item.uploadId = preCheck.data.uploadId;
    item.chunkSize = preCheck.data.chunkSize;
    item.totalChunks = preCheck.data.totalChunks;
    item.uploadedChunks = new Set(preCheck.data.uploadedChunks ?? []);
    item.inflightBytes.clear();
    updateProgress(item);
    persistProgress(item);

    // 预检期间被暂停时，在此等待恢复
    await waitIfPaused(item);
    if (statusOf(item) === 'canceled') return;

    // 3. 上传分片
    item.status = 'uploading';
    startSpeedTicker();
    await uploadAllChunks(item);
    if (statusOf(item) === 'canceled') return;
    if (statusOf(item) === 'paused') {
      // 暂停后由 resume() 重新驱动
      return;
    }

    // 4. 合并
    item.status = 'merging';
    const completeResponse = await completeUpload({ uploadId: item.uploadId, md5: item.md5 });
    const complete = completeResponse.data;
    if (complete.code !== 200) {
      throw new Error(resolveErrorMessage(complete.code, complete.message));
    }

    item.fileId = complete.data.fileId;
    item.instant = complete.data.instant;
    item.progress = 100;
    item.uploadedBytes = item.size;
    item.status = 'success';
    clearPersisted(item);
    notifyUploadSuccess(item.albumId);
  } catch (error) {
    if (item.status === 'canceled') return;
    const code = extractErrorCode(error);
    const backendMessage = extractBackendMessage(error);
    const hasResponse = Boolean((error as { response?: unknown })?.response);
    item.status = 'error';
    item.errorMsg = hasResponse
      ? resolveErrorMessage(code, backendMessage)
      : '网络异常，请检查网络后重试';
  } finally {
    controllers.delete(item.id);
    stopSpeedTickerIfIdle();
    startNext();
  }
}

/**
 * 调度：保证同时最多有 FILE_CONCURRENCY 个文件在处理
 */
function startNext() {
  const running = uploadState.list.filter(
    (item) =>
      item.status === 'hashing' || item.status === 'uploading' || item.status === 'merging',
  ).length;

  if (running >= FILE_CONCURRENCY) return;

  // 只调度尚未被占用的等待项：processItem 在 preCheck 的 await 期间 status 仍是 'waiting'，
  // 若不排除已持 controller 的项，会被 addFiles 的连续 startNext 重复驱动同一文件。
  const next = uploadState.list.find(
    (item) => item.status === 'waiting' && !controllers.has(item.id),
  );
  if (!next) return;

  const controller = new AbortController();
  controllers.set(next.id, controller);
  void processItem(next);
}

// ---------------------------------------------------------------- 对外 API

function createItem(file: File, albumId: number): UploadItem {
  sequence += 1;
  const persisted = readPersisted()[fileKey(file)];
  const reusable = persisted && Date.now() - persisted.ts <= PERSIST_TTL;

  return {
    id: `${Date.now()}-${sequence}`,
    file,
    name: file.name,
    size: file.size,
    albumId,
    status: 'waiting',
    progress: 0,
    hashProgress: 0,
    uploadedBytes: 0,
    speed: 0,
    // 命中本地记录时直接复用指纹与任务，跳过耗时的哈希计算
    md5: reusable ? persisted.md5 : '',
    uploadId: reusable ? persisted.uploadId : '',
    chunkSize: 0,
    totalChunks: 0,
    uploadedChunks: new Set<number>(),
    inflightBytes: new Map<number, number>(),
    errorMsg: '',
    instant: false,
    fileId: '',
    category: detectCategory(file.name),
    thumbnail: '',
  };
}

async function cancelItem(item: UploadItem) {
  item.status = 'canceled';
  controllers.get(item.id)?.abort();
  controllers.delete(item.id);
  if (item.uploadId) {
    try {
      await abortUpload(item.uploadId);
    } catch {
      // 清理失败不影响前端状态
    }
  }
  clearPersisted(item);
}

export const uploadManager = {
  state: uploadState,

  /**
   * 加入上传队列
   * @returns 被拒绝的文件及原因
   */
  addFiles(files: File[] | FileList, albumId: number): string[] {
    const fileArray = Array.from(files);
    const rejected: string[] = [];
    const accepted: UploadItem[] = [];

    fileArray.forEach((file) => {
      const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
      if (!ACCEPT_TYPES.includes(ext)) {
        rejected.push(`${file.name}（格式不支持）`);
        return;
      }
      if (file.size === 0) {
        rejected.push(`${file.name}（文件为空）`);
        return;
      }
      const duplicated = uploadState.list.some(
        (item) =>
          item.name === file.name &&
          item.size === file.size &&
          item.file.lastModified === file.lastModified &&
          item.status !== 'canceled' &&
          item.status !== 'error',
      );
      if (duplicated) {
        return;
      }
      accepted.push(createItem(file, albumId));
    });

    if (accepted.length > 0) {
      uploadState.list.push(...accepted);
      accepted.forEach((item) => generateThumbnail(item));
      for (let i = 0; i < FILE_CONCURRENCY; i += 1) {
        startNext();
      }
    }
    return rejected;
  },

  pause(item: UploadItem) {
    if (item.status === 'uploading' || item.status === 'hashing' || item.status === 'waiting') {
      item.status = 'paused';
    }
  },

  resume(item: UploadItem) {
    if (item.status !== 'paused') return;
    item.status = 'uploading';
    // 分片上传阶段暂停时，原流程仍阻塞在 await 中，只需恢复状态让它继续跑完；
    // 只有在排队阶段被暂停（尚未启动）时，才需要重新调度。
    if (!controllers.has(item.id)) {
      controllers.set(item.id, new AbortController());
      void processItem(item);
    }
  },

  cancel(item: UploadItem) {
    return cancelItem(item);
  },

  retry(item: UploadItem) {
    if (item.status !== 'error') return;
    item.status = 'waiting';
    item.errorMsg = '';
    item.inflightBytes.clear();
    // 保留 md5 与 uploadId：服务端会返回已上传分片，失败重试自动续传
    controllers.set(item.id, new AbortController());
    void processItem(item);
  },

  remove(item: UploadItem) {
    if (item.status !== 'success' && item.status !== 'canceled' && item.status !== 'error') {
      void cancelItem(item);
    }
    const index = uploadState.list.findIndex((i) => i.id === item.id);
    if (index >= 0) {
      disposeThumbnail(item);
      uploadState.list.splice(index, 1);
    }
  },

  pauseAll() {
    uploadState.list.forEach((item) => this.pause(item));
  },

  resumeAll() {
    uploadState.list.filter((item) => item.status === 'paused').forEach((item) => this.resume(item));
  },

  clearCompleted() {
    const completed = uploadState.list.filter(
      (item) => item.status === 'success' || item.status === 'canceled',
    );
    completed.forEach((item) => {
      const index = uploadState.list.findIndex((i) => i.id === item.id);
      if (index >= 0) {
        disposeThumbnail(item);
        uploadState.list.splice(index, 1);
      }
    });
  },

  /** 清空所有已结束（成功/失败/取消）项，关闭面板时调用 */
  clearFinished() {
    const finished = uploadState.list.filter(
      (item) =>
        item.status === 'success' || item.status === 'error' || item.status === 'canceled',
    );
    finished.forEach((item) => {
      const index = uploadState.list.findIndex((i) => i.id === item.id);
      if (index >= 0) {
        disposeThumbnail(item);
        uploadState.list.splice(index, 1);
      }
    });
  },

  /** 是否还有正在进行的上传（用于关闭面板前的确认） */
  hasActive(): boolean {
    return uploadState.list.some(
      (item) =>
        item.status === 'hashing' ||
        item.status === 'uploading' ||
        item.status === 'merging' ||
        item.status === 'paused',
    );
  },
};

pruneExpiredProgress();
