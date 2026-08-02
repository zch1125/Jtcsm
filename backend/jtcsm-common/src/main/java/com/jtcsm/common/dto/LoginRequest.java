package com.jtcsm.common.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信登录请求体
 */
public class LoginRequest {

    /** 微信临时 code（生产环境必传） */
    private String code;

    /** 模拟昵称（开发环境用，不传 code 时使用） */
    private String nickname;

    /** 模拟头像（开发环境用） */
    private String avatar;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
