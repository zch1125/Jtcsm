package com.jtcsm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流 —— 标注在 Controller 方法上
 * <p>
 * 由 RateLimitAspect 拦截，基于 Redis 实现滑动窗口计数，
 * 超限后返回 429 错误。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** Redis key 前缀，为空则使用方法名 */
    String key() default "";

    /** 时间窗口内最大请求次数 */
    int limit() default 10;

    /** 时间窗口（秒） */
    int duration() default 60;
}
