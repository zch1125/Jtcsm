package com.jtcsm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 需要会员权限 —— 标注在 Controller 方法上
 * <p>
 * 由 VipRequiredAspect 拦截，通过 Redis 检查用户 VIP 状态，
 * 非会员返回 402 错误。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VipRequired {

    /** 提示消息 */
    String message() default "该功能需要会员权限";
}
