package com.jtcsm.common.aspect;

import com.jtcsm.common.annotation.VipRequired;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * VIP 会员鉴权切面
 * <p>
 * 拦截所有标注 {@code @VipRequired} 的方法，通过 Redis 中缓存的 VIP 状态判断。
 * 会员充值后由业务层写入 Redis key {@code jtcsm:vip:{userId}}，值为 "1"。
 * </p>
 */
@Aspect
@Component
public class VipRequiredAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(vipRequired)")
    public Object checkVip(ProceedingJoinPoint joinPoint, VipRequired vipRequired) throws Throwable {
        // 1. 检查登录状态
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 2. 从 Redis 读取 VIP 缓存状态
        String vipStatus = stringRedisTemplate.opsForValue().get("jtcsm:vip:" + userId);
        if (!"1".equals(vipStatus)) {
            throw new BusinessException(402, vipRequired.message());
        }

        return joinPoint.proceed();
    }
}
