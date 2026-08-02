package com.jtcsm.api.controller;

import com.jtcsm.api.service.UserPreferenceService;
import com.jtcsm.common.Result;
import com.jtcsm.common.context.UserContext;
import com.jtcsm.common.dto.PreferenceUpdateRequest;
import com.jtcsm.common.entity.UserPreference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 偏好控制器 —— 用户偏好设置
 */
@RestController
@RequestMapping("/api/v1/user")
public class PreferenceController {

    @Autowired
    private UserPreferenceService preferenceService;

    /**
     * 获取当前用户的偏好设置
     * GET /api/v1/user/preference
     */
    @GetMapping("/preference")
    public Result<UserPreference> getPreference() {
        Long userId = UserContext.getUserId();
        UserPreference pref = preferenceService.getPreference(userId);
        return Result.ok(pref);
    }

    /**
     * 更新当前用户的偏好设置（存在则更新，不存在则创建）
     * PUT /api/v1/user/preference
     */
    @PutMapping("/preference")
    public Result<Void> updatePreference(@RequestBody PreferenceUpdateRequest request) {
        Long userId = UserContext.getUserId();
        preferenceService.updatePreference(userId, request);
        return Result.ok();
    }
}
