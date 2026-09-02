package com.memorylink.common;

/**
 * 统一响应体：{ code, message, data }
 * 错误码分段：1xxx 认证、2xxx 参数、3xxx 业务、4xxx 权限、5xxx 系统。
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
