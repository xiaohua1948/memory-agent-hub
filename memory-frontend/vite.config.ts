import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import IconsResolver from 'unplugin-icons/resolver'
import Icons from 'unplugin-icons/vite'

export default defineConfig(({ mode }) => {
  // 读取 .env.development / .env.production 中的变量
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_TARGET || 'http://127.0.0.1:8088'
  const apiBase = env.VITE_API_BASE || '/devApi'

  return {

  // 配置本地服务
  server: {
    host: true, //0.0.0.0或者true为监听所有ip
    port: 8080, //端口号
    open: false, //是否自动打开浏览器
    hmr: true,//热更新
    //反向代理
    proxy: {
      [apiBase]: {
        target: apiTarget, //代理地址 目标后端服务地址
        changeOrigin: true, //开启跨域
        rewrite: (path) => path.replace(new RegExp(`^${apiBase}`), ""), //重写路径，将/devApi替换为""
      },
      "/mockApi": {
        target: "http://127.0.0.1:5000", //代理地址 mock环境后端服务地址
        changeOrigin: true, //开启跨域
        rewrite: (path) => path.replace(/^\/mockApi/, ""), //重写路径，将/mockApi替换为""
      },
    },
  },

  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver(),
      ],
    }),
    Components({
      resolvers: [ElementPlusResolver(),
      IconsResolver({
        // icon的前缀 组件使用{prefix}-{collection}-{icon} eg:i-ep-search
        prefix: 'i'
        // enabledCollections:['ep']  这是可选的，默认启用 Iconify 支持的所有集合，ep指的是element_ui的图标库
        // alias: { park: 'icon-park' } 集合的别名
      }),
      ],
    }),
    Icons({
      scale: 1, // 缩放比 相对1em
      autoInstall: true, // 自动安装
      compiler: 'vue3' // 编译方式
    })
  ],
  
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  }
})





