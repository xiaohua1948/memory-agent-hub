import request from "@/api/request";

export async function selectAll(data: object) {
    return request<API.BaseResponse>('/record/selectAll', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: data,
    });
  }