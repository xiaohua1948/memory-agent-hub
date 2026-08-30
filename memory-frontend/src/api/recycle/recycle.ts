import request from "@/api/request";

export async function selectAllFile(data: object) {
    return request<API.BaseResponse>('/recycle/selectAllFile', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }


  export async function recoverPicture(data: object) {
    return request<API.BaseResponse>('/recycle/recoverPicture', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }



  export async function dropPicture(data: object) {
    return request<API.BaseResponse>('/recycle/dropPicture', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }