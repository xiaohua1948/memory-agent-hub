import request from "@/api/request";
export async function updateUserInfo(data: object) {
    return request<API.BaseResponse>('/user/updateUserInfo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: data,
    });
  }


  export async function getUserInfo() {
    return request<API.BaseResponse>('/user/getUserInfo', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
  
    });
  }


  export async function handleUpdateUserStatus(data: object) {
    return request<API.BaseResponse>('/user/updateUserStatus', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }
  export async function handleAddUserStorage(data: object) {
    return request<API.BaseResponse>('/user/addUserStorage', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }