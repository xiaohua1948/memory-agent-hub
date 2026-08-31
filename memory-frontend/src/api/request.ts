/**
 * 请求封装
 * 1.下载依赖
 * 2.创建文件
 * 3.导入/引入 axios
 * 4.添加默认配置
 * 5.定义返回的数据类型
 * 6.添加拦截器
 * 7.封装请求方法
 * 8.导出/抛出 实例
 */
import axios, { type AxiosInstance, type AxiosResponse, AxiosError, type InternalAxiosRequestConfig, type AxiosRequestConfig } from "axios";
import router from "@/router";
import requestPublicConfig from "./config";
import { ElMessage } from "element-plus";
import { STORAGE_KEYS } from "@/utils/storage";
//自定义请求配置
export interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  isLoading?: boolean
}
//默认配置
const defaultConfig = {
  baseURL: requestPublicConfig.baseUrl,//接口地址 根据不同的环境需要更换（见 config.ts）
  timeout: requestPublicConfig.timeout,//超时时间
  headers: requestPublicConfig.headers,//请求头设置
  withCredentials: true,// 跨域时候允许携带凭证
  isLoading: true,//是否显示loading
}



// 定义接口
// 取消重复请求
const pending = new Map<string, AbortController>();
const getRequestKey = (config: AxiosRequestConfig) =>
  `${config.method}:${config.url}:${JSON.stringify(config.params)}:${JSON.stringify(config.data)}`;
// 移除重复请求
const cancelDuplicateRequests = (config: AxiosRequestConfig) => {
  const key = getRequestKey(config);
  if (pending.has(key)) {
    // 当前请求在 Map 中存在时执行取消操作
    pending.get(key)!.abort('操作太频繁，请稍后再试');
    pending.delete(key);
  }
};



//请求失败状态码统一处理
const checkStatus = (status: number | string,statusText:string) => {
  switch (Number(status)) {
    case 302:
      ElMessage.error('接口重定向了！');
      break;
    case 400:
      ElMessage.error("请求错误" + statusText)
      break;
    case 401:
      ElMessage.error("当前用户未登录或登录过期，请进行登录！")
      localStorage.removeItem(STORAGE_KEYS.USER_INFO);
      localStorage.removeItem(STORAGE_KEYS.TOKEN);
      router.replace({ path: '/login' });
      break;
    case 403:
      ElMessage.error("登录过期,token失效")
      localStorage.removeItem(STORAGE_KEYS.USER_INFO);
      localStorage.removeItem(STORAGE_KEYS.TOKEN);
      setTimeout(() => {
        router.replace({ path: '/login' });
      }, 1000);
      break;
    case 404:
      ElMessage.error("请求的资源不存在")
      break;
    case 500:
      ElMessage.error("服务器发生错误")
      break;
    case 502:
      ElMessage.error("网关错误")
      break;
    case 503:
      ElMessage.error("服务不可用，服务器暂时处于维护中")
      break;
    default:
      ElMessage.error("发生未知错误")
  }
}


//实例化请求配置
const service: AxiosInstance = axios.create(defaultConfig);

//请求拦截器
service.interceptors.request.use((config) => {
  //发送请求前，做些事情 例如取消重复请求、添加token到请求头中、添加loading的加载等
  cancelDuplicateRequests(config);// 取消重复请求
  const controller = new AbortController();
  pending.set(getRequestKey(config), controller);
  config.signal = controller.signal;
  //去localStorage中获取token
  const token = localStorage.getItem("token");
  //判断token是否存在
  if (token) {
    //添加token到请求头中
    config.headers['Authorization'] = token;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});


//响应拦截器 
service.interceptors.response.use((response: AxiosResponse) => {
  //对响应数据进行处理 例如 1、取消loading的加载；2、对返回状态进行判断：如请求错误、请求超时、获取数据失败、暂无数据等
  const { status, data, config,statusText } = response;
  if (config.responseType === 'blob') {
    return response; // 直接返回完整响应对象
  }
  cancelDuplicateRequests(config);
  // 请求成功
  if (status === 200 || status === 204) {
    return Promise.resolve(data);
  } else {
    return Promise.reject(config);
  }
}, (error: AxiosError) => {
  // 请求失败
  const { response } = error;
  if (response) {
    checkStatus(response.status,response.statusText);
  }
});

// 定义泛型请求函数
const request = <T>(url: string, config: AxiosRequestConfig): Promise<T> => {
  return service.request<T>({ url, ...config }).then(response => {
      return response as T; // 确保返回的是响应数据
  });
};
export default request;

