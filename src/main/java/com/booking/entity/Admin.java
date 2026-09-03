package com.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 平台/商家账号
 */
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String username;

    /** 原型阶段明文存储，生产环境请改为 BCrypt 加密 */
    private String password;

    private String nickname;

    /** 角色：platform=平台开发者，merchant=商家 */
    private String role;

    /** 归属商家 id（平台账号为 0） */
    private Integer merchantId;

    /** 商家名称（商家账号） */
    private String merchantName;

    /** 联系人电话 */
    private String contactPhone;

    /** 入驻申请状态：申请中/通过/拒绝 */
    private String applyStatus;

    /** 拒绝原因 */
    private String rejectReason;

    /** 商家介绍 */
    private String description;

    /** 营业状态：1=营业中，0=暂停接单（商家账号） */
    private Integer openStatus;

    /** 账号状态：1=启用，0=停用 */
    private Integer status;

    /** 商家门店地址（文字，可空） */
    private String address;

    /** 门店经纬度（GCJ-02，可空） */
    private Double lng;

    private Double lat;

    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getApplyStatus() { return applyStatus; }
    public void setApplyStatus(String applyStatus) { this.applyStatus = applyStatus; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOpenStatus() { return openStatus; }
    public void setOpenStatus(Integer openStatus) { this.openStatus = openStatus; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }
    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
