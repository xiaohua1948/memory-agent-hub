import request from "@/api/request";
import { type AxiosResponse} from "axios";
// 查询所有相册信息
export async function selectAllAlbum(data: object) {
  return request<API.BaseResponse>('/album/selectAllAlbumInfo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function selectAllLocationAlbum(data: object) {
  return request<API.BaseResponse>('/album/selectAllLocationAlbum', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}


export async function selectAllModelAlbum(data: object) {
  return request<API.BaseResponse>('/album/selectAllModelAlbum', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function selectModelAlbumFileInfo(data: object) {
  return request<API.BaseResponse>('/album/selectModelAlbumFileInfo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}


//创建相册
export async function addAlbum(data: object) {
  return request<API.BaseResponse>('/album/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function createShareAlbumUrl(data: object) {
  return request<API.BaseResponse>('/album/createShareAlbumUrl', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}


// 根据相册id查询相册信息
export async function selectAlbumById(data: object) {
  return request<API.BaseResponse>('/album/selectAlbumById', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
// 更新相册信息
export async function updateAlbumInfo(data: object) {
  return request<API.BaseResponse>('/album/updateAlbumInfo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: data,
  });
}
// 更新相册封面
export async function updateAlbumCover(data: object) {
  return request<API.BaseResponse>('/album/updateAlbumCover', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
// 把图片添加到相册中
export async function addPictureToAlbum(data: object) {
  return request<API.BaseResponse>('/album/addPictureToAlbum', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
// 把图片从相册移除
export async function removePictureFromAlbum(data: object) {
  return request<API.BaseResponse>('/album/removePictureFromAlbum', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

// 删除相册
export async function deleteAlbum(data: object) {
  return request<API.BaseResponse>('/album/deleteAlbum', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function getDownloadAlbumToken(data: object) {
  return request<API.BaseResponse>('/album/getDownloadAlbumToken', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}






