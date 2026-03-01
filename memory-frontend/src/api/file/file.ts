import request from "@/api/request";
import { type AxiosResponse } from "axios";
export async function selectAllFileInfo(data: object) {
  return request<API.BaseResponse>('/file/selectAllFileInfo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

// 定义进度回调类型
type ProgressCallback = (percentage: number) => void;

// 修改请求方法的接口定义
export async function createSimilarFileList(data: object) {
  return request<API.BaseResponse>('/file/createSimilarFileList', {
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data', // 👈 建议修改为适用于文件上传的类型
    },
    timeout: -1,
    data: data,
  });
}
export async function getSimilarFileList(data: object) {
  return request<API.BaseResponse>('/file/getSimilarFileList', {
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data', // 👈 建议修改为适用于文件上传的类型
    },
    timeout: -1,
    data: data,
  });
}

export async function getDownloadToken(data: object) {
  return request<API.BaseResponse>('/file/getDownloadToken', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  })
}

export async function downloadFileByToken(data: object) {
  return request<AxiosResponse>('/file/downloadFileByToken', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    timeout: -1, // 5分钟,
    data: data,
    responseType: 'blob'
  })
}
export async function deleteFileByIds(data: object) {
  return request<API.BaseResponse>('/file/deleteFileByIds', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function selectMetaDataByFileId(data: object) {
  return request<API.BaseResponse>('/file/selectMetaDataByFileId', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}



export async function createShareUrl(data: object) {
  return request<API.BaseResponse>('/file/createShareUrl', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function getShareFileInfo(data: object) {
  return request<API.BaseResponse>('/file/getShareFileInfo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function saveSharePicture(data: object) {
  return request<API.BaseResponse>('/file/saveSharePicture', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  })
}
export async function selectSharedFileMetaIno(data: object) {
  return request<API.BaseResponse>('/file/selectSharedFileMetaIno', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function getDownloadSharedFileToken(data: object) {
  return request<API.BaseResponse>('/file/getDownloadSharedFileToken', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  })
}

export async function addPictureTag(data: object) {
  return request<API.BaseResponse>('/file/addPictureTag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}
export async function addSomePictureTag(data: object) {
  
  return request<API.BaseResponse>('/file/addSomePictureTag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    }, 
    data: data,
  })
}

export async function removePictureTag(data: object) {
  return request<API.BaseResponse>('/file/removePictureTag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function selectFileListByKeyword(data: object) {
  return request<API.BaseResponse>('/search/selectFileListByKeyword', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function selectTagByFileId(data: object) {
  return request<API.BaseResponse>('/file/selectTagByFileId', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}

export async function getPictureTag(data: object) {
  return request<API.BaseResponse>('/file/getPictureTag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
    },
    data: data,
  });
}



