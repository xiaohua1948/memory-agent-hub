/**
 * 公共配置
 */
const requestPublicConfig = {
    baseUrl: import.meta.env.VITE_BASE_URL || "/devApi",
    timeout: 100000,
    headers: {
      Accept: 'application/json, text/plain, */*',
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest'
    },
  };
   
  export default requestPublicConfig;