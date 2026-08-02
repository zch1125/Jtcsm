package com.jtcsm.common.aspect;

import com.jtcsm.common.annotation.RateLimit;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 * <p>
 * 拦截所有标注 {@code @RateLimit} 的方法，基于 Redis INCR + EXPIRE 实现固定窗口计数。
 * Key 格式：{@code jtcsm:rate:{key}:{userId}} 或 {@code jtcsm:rate:{key}:anonymous}。
 * </p>
 */
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 构建限流 key：优先用注解指定的 key，否则用方法名
        String keyPrefix = rateLimit.key();
        if (keyPrefix.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            keyPrefix = signature.getMethod().getName();
        }

        // 2. 附加用户标识（已登录用 userId，未登录用 "anonymous"）
        Long userId = UserContext.getUserId();
        String userSuffix = userId != null ? String.valueOf(userId) : "anonymous";

        String redisKey = "jtcsm:rate:" + keyPrefix + ":" + userSuffix;

        // 3. Redis 原子自增，首次设置过期时间
        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count == null) {
            return joinPoint.proceed();
        }
        if (count == 1) {
            stringRedisTemplate.expire(redisKey, rateLimit.duration(), TimeUnit.SECONDS);
        }

        // 4. 超限则拒绝
        if (count > rateLimit.limit()) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }

        return joinPoint.proceed();
    }
}
