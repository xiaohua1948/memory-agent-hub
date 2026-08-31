// src/utils/storage.ts
// 统一存储键名，避免散落字符串 'token' / 'userInfo' / 'codeCountdown' 与封装键大小写不一致导致登出残留
export const STORAGE_KEYS = {
  USER_INFO: 'USER_INFO',
  TOKEN: 'TOKEN',
  CODE_COUNTDOWN: 'CODE_COUNTDOWN',
  CURRENT_PAGE_SET1: 'CURRENT_PAGE_SET1',
  CURRENT_PAGE_SET2: 'CURRENT_PAGE_SET2',
  CURRENT_PAGE_SET3: 'CURRENT_PAGE_SET3',
  CURRENT_PAGE_SET4: 'CURRENT_PAGE_SET4',
  CURRENT_PAGE_SET5: 'CURRENT_PAGE_SET5',
  CURRENT_PAGE_SET6: 'CURRENT_PAGE_SET6',
} as const;

type StorageKey = typeof STORAGE_KEYS[keyof typeof STORAGE_KEYS];
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