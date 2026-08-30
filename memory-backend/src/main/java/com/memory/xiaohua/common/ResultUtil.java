package com.memory.xiaohua.common;


import com.memory.xiaohua.exception.StatusCode;

/**
 * 响应工具类
 */
public class ResultUtil {

    /**
     * 成功
     *
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data,String message) {
        return new BaseResponse<>(StatusCode.SUCCESS.getCode(), data, message);
    }

    /**
     * 成功
     *
     * @param message 消息
     * @return 响应
     */
    public static  BaseResponse<?> success(String message) {
        return new BaseResponse<>(StatusCode.SUCCESS.getCode(), null,message);
    }

    /**
     * 失败
     *
     * @param statusCode 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(StatusCode statusCode) {
        return new BaseResponse<>(statusCode);
    }

    /**
     * 失败
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 响应
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     *
     * @param statusCode 错误码
     * @return 响应
     */
    public static BaseResponse<?> error(StatusCode statusCode, String message) {
        return new BaseResponse<>(statusCode.getCode(), null, message);
    }
}