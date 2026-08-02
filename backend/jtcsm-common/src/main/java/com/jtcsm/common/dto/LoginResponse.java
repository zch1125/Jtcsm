package com.jtcsm.common.dto;

/**
 * 登录响应体
 */
public class LoginResponse {

    /** JWT token */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 是否 VIP */
    private Boolean isVip;

    public LoginResponse(String token, Long userId, String nickname, String avatar, Boolean isVip) {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.avatar = avatar;
        this.isVip = isVip;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getIsVip() { return isVip; }
    public void setIsVip(Boolean isVip) { this.isVip = isVip; }
}
