import request from "@/api/request";
// 账号注册
export async function accountRegister(data: object) {
  return request<API.BaseResponse>('/auth/accountRegister', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}

// 账号登陆
export async function accountLogin(data: object) {
  return request<API.BaseResponse>('/auth/accountLogin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}
// 获取验证码
export async function getEmailCode(data: object) {
  return request<API.BaseResponse>('/auth/getEmailCode', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}

// 邮箱验证码登陆
export async function codeLogin(data: object) {
  return request<API.BaseResponse>('/auth/codeLogin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}
// 账号退出登录
export async function accountLogout() {
  return request<API.BaseResponse>('/auth/accountLogout', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  });
}


export async function updatePassWord(data: object) {
  return request<API.BaseResponse>('/auth/updatePassWord', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}


export async function updateEmail(data: object) {
  return request<API.BaseResponse>('/auth/updateEmail', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}
