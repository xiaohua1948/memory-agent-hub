/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 接口请求前缀 */
  readonly VITE_API_BASE: string
  /** 后端服务地址（开发环境用于 vite 代理，生产环境由网关转发） */
  readonly VITE_API_TARGET: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module '*.vue' {
    import type { DefineComponent } from 'vue'
    // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
    const component: DefineComponent<{}, {}, any>
    export default component
  }
  
