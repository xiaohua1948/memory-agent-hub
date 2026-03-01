import request from "@/api/request";

export async function selectAllTags() {
  return request<API.BaseResponse>('/visual/selectAllTags', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
   
  });
}
  export async function selectAllLocation() {
    return request<API.BaseResponse>('/visual/selectAllLocations', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
     
    });
  }

  
  export async function selectAllFile() {
    return request<API.BaseResponse>('/visual/selectAllFiles', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    });
  }

