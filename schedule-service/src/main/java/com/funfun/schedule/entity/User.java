package com.funfun.schedule.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Date;

/**
 * User实体类，对应数据库中的user表
 */
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 用户唯一ID

    @Column(name = "unionid", length = 64)
    private String unionid; // 微信/支付宝UnionID（多端统一标识）

    @Column(name = "openid", length = 64, nullable = false, unique = true)
    private String openid; // 小程序平台唯一ID

    @Column(name = "nickname", length = 64)
    private String nickname; // 用户昵称

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl; // 用户头像URL

    @Column(name = "gender")
    private Integer gender; // 性别：0-未知，1-男，2-女

    @Column(name = "phone", length = 20)
    private String phone; // 用户手机号

    @Column(name = "country", length = 32)
    private String country; // 国家/地区

    @Column(name = "province", length = 32)
    private String province; // 省份

    @Column(name = "city", length = 32)
    private String city; // 城市

    @Column(name = "language", length = 16)
    private String language; // 用户语言

    @Column(name = "register_time", nullable = false, updatable = false)
    private Date registerTime; // 注册时间

    @Column(name = "last_login_time")
    private Date lastLoginTime; // 最后登录时间

    @Column(name = "status", nullable = false)
    private Integer status; // 账号状态：0-禁用，1-正常，2-待审核，3-临时封禁

    @Column(name = "user_tag", length = 128)
    private String userTag; // 用户标签

    @Column(name = "ext_info", columnDefinition = "JSON")
    @Type(type = "json")
    private JsonNode extInfo; // 扩展信息

    @Column(name = "delete_flag", nullable = false)
    private Integer deleteFlag; // 逻辑删除：0-未删除，1-已删除

    @Column(name = "inviter_id")
    private Long inviterId; // 邀请者ID

    // 构造方法
    public User() {
        this.language = "zh_CN";
        this.registerTime = new Date();
        this.status = 1;
        this.deleteFlag = 0;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUserTag() {
        return userTag;
    }

    public void setUserTag(String userTag) {
        this.userTag = userTag;
    }

    public JsonNode getExtInfo() {
        return extInfo;
    }

    public void setExtInfo(JsonNode extInfo) {
        this.extInfo = extInfo;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Long getInviterId() {
        return inviterId;
    }

    public void setInviterId(Long inviterId) {
        this.inviterId = inviterId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", unionid='" + unionid + '\'' +
                ", openid='" + openid + '\'' +
                ", nickname='" + nickname + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", gender=" + gender +
                ", phone='" + phone + '\'' +
                ", country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", city='" + city + '\'' +
                ", language='" + language + '\'' +
                ", registerTime=" + registerTime +
                ", lastLoginTime=" + lastLoginTime +
                ", status=" + status +
                ", userTag='" + userTag + '\'' +
                ", deleteFlag=" + deleteFlag +
                ", inviterId=" + inviterId +
                '}';
    }
}