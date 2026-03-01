import request from "@/api/request";
export async function selectAllPersonAlbum(data: object) {
    return request<API.BaseResponse>('/person/selectAllPersonAlbum', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }


  export async function selectPersonAlbumFileInfo(data: object) {
    return request<API.BaseResponse>('/person/selectPersonAlbumFileInfo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }
  export async function selectPersonById(data: object) {
    return request<API.BaseResponse>('/person/selectPersonById', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }
  export async function selectAllPersonCover(data: object) {
    return request<API.BaseResponse>('/person/selectAllPersonCover', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }
  export async function removePersonPicture(data: object) {
    return request<API.BaseResponse>('/person/removePersonPicture', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }
  export async function updatePersonName(data: object) {
    return request<API.BaseResponse>('/person/updatePersonName', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: data,
    });
  }
  
  export async function updatePersonInfo(data: object) {
    return request<API.BaseResponse>('/person/updatePersonInfo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: data,
    });
  }


  
  export async function mergePerson(data: object) {
    return request<API.BaseResponse>('/person/mergePerson', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }


  export async function hiddenPerson(data: object) {
    return request<API.BaseResponse>('/person/hiddenPerson', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }