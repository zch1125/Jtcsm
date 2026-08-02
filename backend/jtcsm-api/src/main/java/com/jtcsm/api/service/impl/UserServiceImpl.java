package com.jtcsm.api.service.impl;

import com.jtcsm.api.mapper.UserMapper;
import com.jtcsm.api.service.UserService;
import com.jtcsm.common.dto.LoginRequest;
import com.jtcsm.common.dto.LoginResponse;
import com.jtcsm.common.dto.UserUpdateRequest;
import com.jtcsm.common.entity.User;
import com.jtcsm.common.exception.BusinessException;
import com.jtcsm.common.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

@Override
public LoginResponse login(LoginRequest request) {
        // 开发模式：如果未传 code 或 code 为 mock，则执行模拟登录
        String code = request.getCode();
        boolean isMock = (code == null || code.isEmpty() || "mock".equals(code));

        User user;
        if (isMock) {
            // 模拟登录：按固定 openid 查找或创建用户
            String mockOpenId = "mock_openid";
            user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getOpenid, mockOpenId));
            if (user == null) {
                user = new User();
                user.setOpenid(mockOpenId);
                user.setNickname(request.getNickname() != null ? request.getNickname() : "模拟用户");
                user.setAvatar(request.getAvatar() != null ? request.getAvatar() : "");
                user.setGender(0);
                user.setIsVip(0);
                user.setStatus(1);
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userMapper.insert(user);
                log.info("创建模拟用户: id={}, nickname={}", user.getId(), user.getNickname());
            }
        } else {
            // 生产模式：通过微信 code 换取 openId（对接微信登录接口）
            // 临时用 code 作为 openid 占位
            String openId = "wx_" + code;
            user = userMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                            .eq(User::getOpenid, openId));
            if (user == null) {
                throw new BusinessException(400, "用户不存在，请先注册");
            }
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getOpenid());
        log.info("用户登录成功: userId={}, isMock={}", user.getId(), isMock);

        return new LoginResponse(token, user.getId(), user.getNickname(),
                user.getAvatar(), user.getIsVip() == 1);
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    @Override
    public void updateProfile(Long userId, UserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        // 只更新请求中提供了值的字段，未传则不覆盖
        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户信息更新: userId={}", userId);
    }
}
