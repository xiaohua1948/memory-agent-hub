import request from "@/api/request";




  export async function selectAllTags() {
    return request<API.BaseResponse>('/visual/selectAllTags', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
     
    });
  }

  
  export async function selectAllLocations() {
    return request<API.BaseResponse>('/visual/selectAllLocations', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
     
    });
  }


  export async function selectAllModels() {
    return request<API.BaseResponse>('/search/selectAllModels', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
     
    });
  }
  
