/// <reference lib="webworker" />
import SparkMD5 from 'spark-md5';

export interface HashWorkerRequest {
  file: File;
  chunkSize: number;
}

export type HashWorkerResponse =
  | { type: 'progress'; progress: number }
  | { type: 'done'; md5: string }
  | { type: 'error'; message: string };

/**
 * 在 Worker 中增量计算整个文件的 MD5。
 *
 * 放在 Worker 的原因：大文件哈希是 CPU 密集操作，放在主线程会让页面卡死，
 * 用户会以为"网页崩了"。同时这里计算的是<b>全文件</b> MD5——
 * 只算首片会导致秒传误判，也无法与服务端去重逻辑对齐。
 */
self.onmessage = (event: MessageEvent<HashWorkerRequest>) => {
  const { file, chunkSize } = event.data;
  const spark = new SparkMD5.ArrayBuffer();
  const reader = new FileReader();
  const totalChunks = Math.max(1, Math.ceil(file.size / chunkSize));
  let currentChunk = 0;

  reader.onerror = () => {
    const response: HashWorkerResponse = { type: 'error', message: '文件读取失败，请重试' };
    self.postMessage(response);
  };

  reader.onload = (e) => {
    spark.append(e.target?.result as ArrayBuffer);
    currentChunk += 1;

    if (currentChunk < totalChunks) {
      const progressResponse: HashWorkerResponse = {
        type: 'progress',
        progress: currentChunk / totalChunks,
      };
      self.postMessage(progressResponse);
      readNext();
    } else {
      const doneResponse: HashWorkerResponse = { type: 'done', md5: spark.end() };
      self.postMessage(doneResponse);
    }
  };

  const readNext = () => {
    const start = currentChunk * chunkSize;
    const end = Math.min(start + chunkSize, file.size);
    reader.readAsArrayBuffer(file.slice(start, end));
  };

  readNext();
};
