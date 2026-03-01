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
import axios, { type AxiosInstance, type AxiosResponse, AxiosError, type InternalAxiosRequestConfig, type AxiosRequestConfig, type Method } from "axios";
import router from "@/router";
import requestPublicConfig from "./config";
import { ElMessage } from "element-plus";
//自定义请求配置
export interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  isLoading?: boolean
  encrypt?: boolean,
 
}
//默认配置
const defaultConfig = {
  baseURL: requestPublicConfig.baseUrl,//接口地址  TODO: 根据不同的环境需要更换
  timeout: requestPublicConfig.timeout,//超时时间
  headers: requestPublicConfig.headers,//请求头设置
  withCredentials: true,// 跨域时候允许携带凭证
  isLoading: true,//是否显示loading
  encrypt: true,//加解密开关
}



// 定义接口
interface PendingType {
  url?: string;
  method?: Method;
  params: any;
  data: any;
  cancel: any;
}

// 取消重复请求
const pending: Array<PendingType> = [];
const CancelToken = axios.CancelToken;
// 移除重复请求
const cancelDuplicateRequests = (config: AxiosRequestConfig) => {
  for (const key in pending) {
    const item: number = +key;
    const list: PendingType = pending[key];
    // 当前请求在数组中存在时执行取消操作
    if (list.url === config.url && list.method === config.method && JSON.stringify(list.params) === JSON.stringify(config.params) && JSON.stringify(list.data) === JSON.stringify(config.data)) {
      // 执行取消操作
      list.cancel('操作太频繁，请稍后再试');
      // 从数组中移除记录
      pending.splice(item, 1);
    }
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
      localStorage.removeItem("userInfo");
      localStorage.removeItem("token");
      // router.replace({
      //   path: '/login',
      // });
      break;
    case 403:
      ElMessage.error("登录过期,token失效")
      localStorage.removeItem("userInfo");
      // setTimeout(() => {
      //   router.replace({
      //     path: '/login',
      //   });
      // }, 1000);
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
  config.cancelToken = new CancelToken((msg) => {
    pending.push({
      url: config.url,
      method: config.method as Method,
      params: config.params,
      data: config.data,
      cancel: msg,
    });
    //去localStorage中获取token
    let token = localStorage.getItem("token");
    //判断token是否存在
    if (token) {
      //添加token到请求头中
      config.headers['Authorization'] = token;
    }
  });
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

