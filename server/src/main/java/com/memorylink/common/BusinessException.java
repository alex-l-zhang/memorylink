package com.memorylink.common;

/**
 * 业务异常：携带统一错误码（1xxx 认证 / 2xxx 参数 / 3xxx 业务 / 4xxx 权限 / 5xxx 系统）。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
