import request from '@/utils/request'


export function selectRecord(data) {
    return request({
        url: '/record/admin/selectAll',
        method: 'get',
        params: data
    })
}

export function setFile(data) {
    return request({
        url: '/file/admin/set',
        method: 'post',
        data
    })
}

export function selectAllFile(data) {
    return request({
        url: '/file/admin/selectAllFile',
        method: 'get',
        params: data
    })
}

// 查询合集
export function selectAllCollection(data) {
    return request({
        url: '/collection/selectAll',
        method: 'get',
        params: data
    })
}

export function getAddFileToCollection(data) {
    return request({
        url: '/collection/admin/addFile',
        method: 'get',
        params: data
    })
}


export function selectAllAudit(data) {
    return request({
        url: '/audit/admin/selectAll',
        method: 'get',
        params: data
    })
}


export function setAudit(data) {
    return request({
        url: '/audit/admin/setAudit',
        method: 'get',
        params: data
    })
}


export function createCollection(data) {
    return request({
        url: 'collection/admin/add',
        method: 'post',
        data
    })
}










