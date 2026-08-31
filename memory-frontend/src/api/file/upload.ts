import uploadRequest from '@/utils/upload/request';

/** 后端统一响应结构 */
export interface UploadResponse<T> {
  code: number;
  message: string;
  data: T;
}

/** 预检请求参数 */
export interface PreCheckParams {
  filename: string;
  size: number;
  md5: string;
  lastModified: string;
  albumId: number;
  /** 断点续传时带上，用于复用已有上传任务 */
  uploadId?: string;
}

/** 预检结果 */
export interface PreCheckResult {
  /** true 表示秒传成功，无需再传分片 */
  uploaded: boolean;
  fileId: string;
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  /** 已上传的分片序号，断点续传时跳过 */
  uploadedChunks: number[];
  expiresAt: number;
}

/** 分片上传结果 */
export interface ChunkResult {
  chunkIndex: number;
  uploadedCount: number;
  totalChunks: number;
  allChunksDone: boolean;
}

/** 合并结果 */
export interface CompleteResult {
  fileId: string;
  originFileName: string;
  category: string;
  fileUrl: string;
  thumbnailUrl: string;
  width: number;
  height: number;
  size: number;
  instant: boolean;
}

/**
 * 上传错误码（与后端 StatusCode 410xx 对应）
 */
export const UploadErrorCode = {
  FILE_EMPTY: 41001,
  FILE_TYPE_NOT_SUPPORT: 41002,
  FILE_TOO_LARGE: 41003,
  CHUNK_INDEX_INVALID: 41005,
  CHUNK_SIZE_INVALID: 41006,
  CHUNK_MISSING: 41007,
  MD5_MISMATCH: 41008,
  UPLOAD_TASK_NOT_FOUND: 41009,
  STORAGE_ERROR: 41010,
  UPLOAD_MERGE_FAILED: 41011,
  UPLOAD_CONCURRENT_LIMIT: 41012,
  UPLOAD_PARAM_MISSING: 41013,
} as const;

/** 面向用户的错误文案 */
const ERROR_MESSAGES: Record<number, string> = {
  41001: '文件内容为空',
  41002: '文件格式不支持',
  41003: '文件超过大小限制',
  41005: '分片序号错误',
  41006: '分片大小不符合约定',
  41007: '分片缺失，请重新上传',
  41008: '文件校验失败，内容可能已损坏',
  41009: '上传任务已过期，请重新上传',
  41010: '存储服务异常，请稍后重试',
  41011: '文件合并失败，请重试',
  41012: '同时上传的文件数量过多，请稍后再试',
  41013: '上传参数缺失',
};

/** 这些错误重试没有意义，应直接暴露给用户 */
const NON_RETRYABLE_CODES = new Set<number>([
  UploadErrorCode.FILE_EMPTY,
  UploadErrorCode.FILE_TYPE_NOT_SUPPORT,
  UploadErrorCode.FILE_TOO_LARGE,
  UploadErrorCode.MD5_MISMATCH,
  UploadErrorCode.UPLOAD_PARAM_MISSING,
  UploadErrorCode.UPLOAD_CONCURRENT_LIMIT,
]);

/**
 * 解析友好错误文案，优先使用后端返回的 message
 */
export function resolveErrorMessage(code?: number, backendMessage?: string): string {
  if (code && ERROR_MESSAGES[code]) {
    return ERROR_MESSAGES[code];
  }
  if (backendMessage) {
    return backendMessage;
  }
  return '上传失败，请稍后重试';
}

/**
 * 判断是否值得自动重试：
 * 网络异常/5xx/存储临时故障可重试；参数与校验类错误重试无意义。
 */
export function isRetryableError(code?: number, hasResponse?: boolean): boolean {
  // 无响应 = 网络中断或超时，值得重试
  if (!hasResponse) {
    return true;
  }
  if (code === undefined) {
    return true;
  }
  if (NON_RETRYABLE_CODES.has(code)) {
    return false;
  }
  if (code >= 500) {
    return true;
  }
  // 其余业务错误（如任务不存在）重试通常无效，但分片级重试成本极低，允许重试一次
  return true;
}

/** 从 axios 错误中提取业务错误码 */
export function extractErrorCode(error: unknown): number | undefined {
  const response = (error as { response?: { data?: { code?: number } } })?.response;
  return response?.data?.code;
}

/** 从 axios 错误中提取后端 message */
export function extractBackendMessage(error: unknown): string | undefined {
  const response = (error as { response?: { data?: { message?: string } } })?.response;
  return response?.data?.message;
}

/**
 * 阶段一：预检（秒传判定 / 创建任务 / 获取续传进度）
 */
export function preCheckUpload(data: PreCheckParams) {
  return uploadRequest.post<UploadResponse<PreCheckResult>>('/file/upload/pre-check', data);
}

/**
 * 阶段二：上传单个分片
 */
export function uploadChunk(
  params: { uploadId: string; chunkIndex: number; chunk: Blob },
  onProgress?: (loaded: number) => void,
  signal?: AbortSignal,
) {
  const formData = new FormData();
  formData.append('uploadId', params.uploadId);
  formData.append('chunkIndex', String(params.chunkIndex));
  formData.append('chunk', params.chunk);

  return uploadRequest.post<UploadResponse<ChunkResult>>('/file/upload/chunk', formData, {
    signal,
    timeout: 0,
    onUploadProgress: (event) => {
      onProgress?.(event.loaded ?? 0);
    },
  });
}

/**
 * 阶段三：合并分片
 */
export function completeUpload(data: { uploadId: string; md5: string }) {
  return uploadRequest.post<UploadResponse<CompleteResult>>('/file/upload/complete', data);
}

/**
 * 取消上传，清理服务端残留分片
 */
export function abortUpload(uploadId: string) {
  return uploadRequest.delete<UploadResponse<null>>('/file/upload/abort', {
    params: { uploadId },
  });
}
