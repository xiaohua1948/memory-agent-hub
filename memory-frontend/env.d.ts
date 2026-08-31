/// <reference types="vite/client" />

// 确保 import.meta.env 在编辑器中可用（Vite 会在构建时注入，
// 此处补充类型声明以兼容未安装依赖或引用未自动加载的场景）
interface ImportMetaEnv {
  readonly [key: string]: any
  readonly BASE_URL: string
  readonly MODE: string
  readonly DEV: boolean
  readonly PROD: boolean
  readonly VITE_BASE_URL?: string
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
  
