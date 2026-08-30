// src/utils/storage.ts
type StorageKey = 'USER_INFO' | 'TOKEN'|'CURRENT_PAGE_SET1'|'CURRENT_PAGE_SET2'|'CURRENT_PAGE_SET3'|'CURRENT_PAGE_SET4'|'CURRENT_PAGE_SET5'|'CURRENT_PAGE_SET6'; // 定义可用的存储键名
//CURRENT_PAGE_SET1: 首页 相册 .... 
//CURRENT_PAGE_SET2: 最近上传
//CURRENT_PAGE_SET3: 图片相似
//CURRENT_PAGE_SET4: 图片回收站
export const storage = {
  set<T>(key: StorageKey, value: T): void {
    localStorage.setItem(key, JSON.stringify(value));
  },
  get<T>(key: StorageKey): T | null {
    const data = localStorage.getItem(key);
    return data ? JSON.parse(data) : null;
  },
  remove(key: StorageKey): void {
    localStorage.removeItem(key);
  },
  clear(): void {
    localStorage.clear();
  }
};     