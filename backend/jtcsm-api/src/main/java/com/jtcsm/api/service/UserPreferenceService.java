package com.jtcsm.api.service;

import com.jtcsm.common.dto.PreferenceUpdateRequest;
import com.jtcsm.common.entity.UserPreference;

/**
 * 用户偏好服务接口
 */
public interface UserPreferenceService {

    /** 获取偏好设置 */
    UserPreference getPreference(Long userId);

    /** 更新偏好设置（存在则更新，不存在则创建） */
    void updatePreference(Long userId, PreferenceUpdateRequest request);
}