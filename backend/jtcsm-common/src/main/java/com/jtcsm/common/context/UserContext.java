package com.jtcsm.common.context;

/**
 * 用户上下文 —— 通过 ThreadLocal 持有当前请求的用户信息
 * <p>
 * JWT 拦截器在解析 token 后写入，请求结束时清理。
 * </p>
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> OPEN_ID_HOLDER = new ThreadLocal<>();

    /** 设置当前用户 ID */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /** 获取当前用户 ID（可能为 null，未登录时） */
    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /** 设置当前用户 openId */
    public static void setOpenId(String openId) {
        OPEN_ID_HOLDER.set(openId);
    }

    /** 获取当前用户 openId */
    public static String getOpenId() {
        return OPEN_ID_HOLDER.get();
    }

    /** 当前是否已登录 */
    public static boolean isLogin() {
        return USER_ID_HOLDER.get() != null;
    }

    /** 请求结束后清理，防止内存泄漏 */
    public static void clear() {
        USER_ID_HOLDER.remove();
        OPEN_ID_HOLDER.remove();
    }
}
