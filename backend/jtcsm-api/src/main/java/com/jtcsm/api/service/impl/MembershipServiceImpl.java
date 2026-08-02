package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.MembershipPlanMapper;
import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.api.mapper.UserMembershipMapper;
import com.jtcsm.api.service.MembershipService;
import com.jtcsm.common.dto.MembershipStatusVO;
import com.jtcsm.common.entity.MembershipPlan;
import com.jtcsm.common.entity.User;
import com.jtcsm.common.entity.UserMembership;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员服务实现
 */
@Service
public class MembershipServiceImpl implements MembershipService {

    private static final Logger log = LoggerFactory.getLogger(MembershipServiceImpl.class);

    @Autowired
    private MembershipPlanMapper planMapper;

    @Autowired
    private UserMembershipMapper userMembershipMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<MembershipPlan> listPlans() {
        // 查询所有已启用的套餐，按价格升序排列
        return planMapper.selectList(
                new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getIsEnabled, 1)
                        .orderByAsc(MembershipPlan::getPrice));
    }

    @Override
    public MembershipStatusVO getStatus(Long userId) {
        MembershipStatusVO vo = new MembershipStatusVO();

        User user = userMapper.selectById(userId);
        if (user == null) {
            vo.setIsVip(false);
            return vo;
        }

        // 判断 VIP 是否有效（isVip=1 且未过期）
        boolean isVip = user.getIsVip() != null && user.getIsVip() == 1
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
        vo.setIsVip(isVip);
        vo.setVipExpireTime(user.getVipExpireTime());

        if (isVip) {
            // 查询当前有效的会员记录，计算剩余天数
            UserMembership membership = userMembershipMapper.selectOne(
                    new LambdaQueryWrapper<UserMembership>()
                            .eq(UserMembership::getUserId, userId)
                            .eq(UserMembership::getStatus, 1));
            if (membership != null) {
                long remainingDays = Duration.between(LocalDateTime.now(), membership.getExpireTime()).toDays();
                vo.setRemainingDays(Math.max(0, remainingDays));

                MembershipPlan plan = planMapper.selectById(membership.getPlanId());
                if (plan != null) {
                    vo.setPlanName(plan.getName());
                }
            }
        }

        log.debug("VIP status: userId={}, isVip={}", userId, isVip);
        return vo;
    }
}
