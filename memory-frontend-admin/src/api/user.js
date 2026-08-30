import request from '@/utils/request'

export function login(data) {
  return request({
    url: '/auth/accountLogin',
    method: 'post',
    data,
   
  })
}

export function getInfo() {
  return request({
    url: '/user/getUserInfo',
    method: 'get',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded' // 简单头部
    }
  })
}

// 修改用户个人信息
export function updateUserProfile(data) {
  return request({
    url: '/system/user/profile',
    method: 'put',
    data: data
  })
}
// 用户密码重置
export function updateUserPwd(oldPassword, newPassword) {
  const data = {
    oldPassword,
    newPassword
  }
  return request({
    url: '/system/user/profile/updatePwd',
    method: 'put',
    data: data
  })
}

updateUserProfile
// 用户头像上传
export function uploadAvatar(data) {
  return request({
    url: '/system/user/profile/avatar',
    method: 'post',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    data: data
  })
}
// 用户密码重置
export function resetUserPwd(userId, password) {
  const data = {
    userId,
    password
  }
  return request({
    url: '/system/user/resetPwd',
    method: 'put',
    data: data
  })
}

export function selectVisualData() {
  return request({
    url: '/admin/selectVisualData',
    method: 'get',
})
}

export function selectAllAdmin() {
  return request({
      url: '/admin/selectAllAdmin',
      method: 'get',
  
  })
}
export function selectAllUserInfo() {
  return request({
      url: '/admin/selectAllUserInfo',
      method: 'get',
  
  })
}

export function selectAllRecord(data) {
  return request({
      url: '/admin/selectAllRecord',
      method: 'post',
      data:data,
      headers: { 'Content-Type': 'application/json' },
  })
  
}


export function updateUserInfo(data) {
  return request({
    url: '/admin/updateUserInfo',
      method: 'post',
      data: data
  })
  
}
export function sendEmailCode(data) {
  return request({
    url: '/auth/getEmailCode',
      method: 'post',
      data: data
  })
}
export function addAdmin(data) {
  return request({
    url: '/admin/addAdmin',
      method: 'post',
      data: data
  })
}

