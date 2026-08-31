import axios from 'axios';
import requestPublicConfig from '@/api/config';

/**
 * 上传专用 axios 实例
 *
 * 与全局 request 实例的区别（这三点正是原上传链路吞掉错误的原因）：
 * 1. 不做"重复请求取消"——分片上传会并发发起大量同 URL 请求，
 *    而全局实例用 JSON.stringify(data) 生成去重键，FormData 序列化后恒为 {}，
 *    会导致所有分片请求被判定为重复而互相取消；
 * 2. 错误不被吞掉——全局实例 response 拦截器的错误分支没有 return，
 *    Promise 会 resolve(undefined)，调用方永远感知不到失败；
 * 3. 不设超时——大分片在弱网下耗时可能很长，超时中断会白白浪费已传数据。
 */
const uploadRequest = axios.create({
  baseURL: requestPublicConfig.baseUrl,
  timeout: 0,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
});

uploadRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = token;
  }
  return config;
});

export default uploadRequest;
