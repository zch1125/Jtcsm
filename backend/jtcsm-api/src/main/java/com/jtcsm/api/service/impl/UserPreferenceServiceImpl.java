package com.jtcsm.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jtcsm.api.mapper.UserPreferenceMapper;
import com.jtcsm.api.service.UserPreferenceService;
import com.jtcsm.common.dto.PreferenceUpdateRequest;
import com.jtcsm.common.entity.UserPreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户偏好服务实现
 */
@Service
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceServiceImpl.class);

    @Autowired
    private UserPreferenceMapper preferenceMapper;

    @Override
    public UserPreference getPreference(Long userId) {
        // 查询用户偏好，不存在时返回空对象
        UserPreference pref = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId));
        return pref != null ? pref : new UserPreference(); // 无偏好时返回空对象
    }

    @Override
    public void updatePreference(Long userId, PreferenceUpdateRequest request) {
        UserPreference pref = preferenceMapper.selectOne(
                new LambdaQueryWrapper<UserPreference>()
                        .eq(UserPreference::getUserId, userId));

        if (pref == null) {
            // 偏好不存在则创建新记录
            pref = new UserPreference();
            pref.setUserId(userId);
            pref.setTaste(request.getTaste());
            pref.setTaboo(request.getTaboo());
            pref.setCuisine(request.getCuisine());
            pref.setDifficulty(request.getDifficulty());
            pref.setCookMethod(request.getCookMethod());
            pref.setCreateTime(LocalDateTime.now());
            pref.setUpdateTime(LocalDateTime.now());
            preferenceMapper.insert(pref);
            log.info("创建用户偏好: userId={}", userId);
        } else {
            // 偏好已存在，仅更新有值的字段
            if (request.getTaste() != null) pref.setTaste(request.getTaste());
            if (request.getTaboo() != null) pref.setTaboo(request.getTaboo());
            if (request.getCuisine() != null) pref.setCuisine(request.getCuisine());
            if (request.getDifficulty() != null) pref.setDifficulty(request.getDifficulty());
            if (request.getCookMethod() != null) pref.setCookMethod(request.getCookMethod());
            pref.setUpdateTime(LocalDateTime.now());
            preferenceMapper.updateById(pref);
            log.info("更新用户偏好: userId={}", userId);
        }
    }
}
