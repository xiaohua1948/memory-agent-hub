import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import 'ant-design-vue/dist/antd.css'
import {Statistic,Tooltip,Button,Input,Checkbox,Form,Layout,Menu,Icon,Breadcrumb,Dropdown,Avatar} from 'ant-design-vue';
import echarts from 'echarts'
// 导入工具类cooike.js
import cookie from './utils/cookie'

import './assets/icons/index'

// 导入ajax
import axios from 'axios';
//设置携带cooike凭证。保证session一致
axios.defaults.withCredentials = true;
Vue.prototype.$cookie = cookie;
Vue.prototype.$echarts = echarts
Vue.prototype.axios = axios;
// 服务端请求路径
Vue.prototype.$serveUrL = 'http://localhost:8080'


Vue.use(ElementUI);
Vue.use(Statistic);
Vue.use(Tooltip);
Vue.use(Button);
Vue.use(Input);
Vue.use(Checkbox);
Vue.use(Form);
Vue.use(Layout);
Vue.use(Menu);
Vue.use(Icon);
Vue.use(Breadcrumb);
Vue.use(Dropdown);
Vue.use(Avatar);

// Vue.use(antd);
// 阻止 vue 在启动时生成生产提示
Vue.config.productionTip = false; 

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
