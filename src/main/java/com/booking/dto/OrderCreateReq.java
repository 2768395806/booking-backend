package com.booking.dto;

/**
 * 下单请求
 */
public class OrderCreateReq {

    private Integer houseId;
    private Integer roomId;
    private String guestName;
    private String phone;
    private String checkIn;
    private String checkOut;
    private Integer nights;
    private String remark;
    /** 微信 openid（登录用户下单时携带，便于后续按用户查询订单） */
    private String openid;

    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }

    public Integer getHouseId() { return houseId; }
    public void setHouseId(Integer houseId) { this.houseId = houseId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCheckIn() { return checkIn; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public String getCheckOut() { return checkOut; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public Integer getNights() { return nights; }
    public void setNights(Integer nights) { this.nights = nights; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
