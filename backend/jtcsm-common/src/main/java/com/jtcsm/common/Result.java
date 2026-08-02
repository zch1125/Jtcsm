package com.jtcsm.common;

import java.io.Serializable;

/**
 * 统一响应体
 * <p>
 * 所有 API 统一使用此结构返回，格式：{code, message, data}
 * </p>
 *
 * @param <T> 响应数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200成功 / 401未登录 / 402需会员 / 403无权限 / 404不存在 / 429限流 / 500服务器错误 */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    // ==================== 构造方法 ====================

    private Result() {}

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ==================== 静态工厂方法 ====================

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(200, "success", null);
    }

    /** 成功（有数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /** 成功（自定义消息 + 数据） */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(200, message, data);
    }

    /** 失败 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /** 未登录 */
    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    /** 需要会员 */
    public static <T> Result<T> vipRequired() {
        return new Result<>(402, "该功能需要会员权限", null);
    }

    /** 无权限 */
    public static <T> Result<T> forbidden() {
        return new Result<>(403, "无权限访问", null);
    }

    /** 资源不存在 */
    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    /** 限流 */
    public static <T> Result<T> rateLimited() {
        return new Result<>(429, "请求过于频繁，请稍后再试", null);
    }

    /** 服务器错误 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // ==================== Getter / Setter ====================

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
