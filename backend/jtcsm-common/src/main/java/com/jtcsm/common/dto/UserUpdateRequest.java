package com.jtcsm.common.dto;

/**
 * 更新个人信息请求体
 */
public class UserUpdateRequest {

    private String nickname;
    private String avatar;
    private Integer gender;
    private String phone;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
