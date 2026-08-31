import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import uploader from 'vue-simple-uploader';
import 'vue-simple-uploader/dist/style.css';
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
const app = createApp(App)
app.use(router)
app.use(uploader)
app.use(ElementPlus, { locale: zhCn })
// 按需引入图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')