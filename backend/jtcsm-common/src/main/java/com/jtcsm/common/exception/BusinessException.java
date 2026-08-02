package com.jtcsm.common.exception;

/**
 * 业务异常 —— 用于 Service 层抛出，由 GlobalExceptionHandler 统一处理
 */
public class BusinessException extends RuntimeException {

    /** 状态码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public int getCode() { return code; }
}
