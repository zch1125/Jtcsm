package com.jtcsm.api.service;

import com.jtcsm.common.dto.LoginRequest;
import com.jtcsm.common.dto.LoginResponse;
import com.jtcsm.common.dto.UserUpdateRequest;
import com.jtcsm.common.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 微信登录 / 模拟登录 */
    LoginResponse login(LoginRequest request);

    /** 获取个人信息 */
    User getProfile(Long userId);

    /** 更新个人信息 */
    void updateProfile(Long userId, UserUpdateRequest request);
}