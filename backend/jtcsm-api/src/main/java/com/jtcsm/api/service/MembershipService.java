package com.jtcsm.api.service;

import com.jtcsm.common.dto.MembershipStatusVO;
import com.jtcsm.common.entity.MembershipPlan;

import java.util.List;

/**
 * 会员服务接口 —— 套餐列表和会员状态查询
 */
public interface MembershipService {

    List<MembershipPlan> listPlans();

    MembershipStatusVO getStatus(Long userId);

}
