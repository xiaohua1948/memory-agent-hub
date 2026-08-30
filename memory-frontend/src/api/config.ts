/**
 * 公共配置
 */
const requestPublicConfig = {
    // 接口前缀，来自 .env.development / .env.production 中的 VITE_API_BASE
    baseUrl: import.meta.env.VITE_API_BASE,
    timeout: 100000,
    headers: {
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest'
    },
  };
   
  export default requestPublicConfig;