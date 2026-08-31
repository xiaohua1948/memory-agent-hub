package com.memory.xiaohua.exception;

import lombok.Getter;

@Getter
public enum StatusCode {


    SUCCESS(200, "ok"),
    PARAMS_ERROR(400, "请求参数错误"),
    NOT_LOGIN_ERROR(401, "未登录"),
    NO_AUTH_ERROR(402, "无权限"),
    FORBIDDEN_ERROR(403, "禁止访问"),
    NOT_FOUND_ERROR(404, "请求数据不存在"),
    SYSTEM_ERROR(500, "系统内部异常"),
    OPERATION_ERROR(501, "操作失败"),

    // ===== 上传相关错误码 (410xx) =====
    FILE_EMPTY(41001, "文件不能为空"),
    FILE_TYPE_NOT_SUPPORT(41002, "文件格式不支持"),
    FILE_TOO_LARGE(41003, "文件超过大小限制"),
    CHUNK_INDEX_INVALID(41005, "分片序号非法"),
    CHUNK_SIZE_INVALID(41006, "分片大小非法"),
    CHUNK_MISSING(41007, "存在缺失的分片，请重新上传"),
    MD5_MISMATCH(41008, "文件校验失败，内容可能被篡改"),
    UPLOAD_TASK_NOT_FOUND(41009, "上传任务不存在或已过期"),
    STORAGE_ERROR(41010, "存储服务异常"),
    UPLOAD_MERGE_FAILED(41011, "文件合并失败"),
    UPLOAD_CONCURRENT_LIMIT(41012, "同时上传的文件数量过多"),
    UPLOAD_PARAM_MISSING(41013, "缺少必要的上传参数");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 信息
     */
    private final String message;

    StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}