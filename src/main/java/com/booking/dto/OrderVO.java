package com.booking.dto;

import com.booking.entity.BookingOrder;

import java.math.BigDecimal;

/**
 * 订单视图：订单 + 房源/房型冗余信息（列表展示用）
 */
public class OrderVO {

    private Integer id;
    private String orderNo;
    private Integer houseId;
    private Integer roomId;
    private String houseName;
    private String roomName;
    private String imgUrl;
    private String guestName;
    private String phone;
    private String checkIn;
    private String checkOut;
    private Integer nights;
    private BigDecimal amount;
    private String status;
    private String statusText;
    private String remark;
    private String createTime;
    private String confirmTime;
    private String checkInTime;
    private String completeTime;
    private String cancelReason;
    /** 商家名称（C 端联系商家用） */
    private String merchantName;
    /** 商家联系电话（C 端一键拨号） */
    private String merchantPhone;

    public static OrderVO from(BookingOrder o) {
        OrderVO vo = new OrderVO();
        vo.id = o.getId();
        vo.orderNo = o.getOrderNo();
        vo.houseId = o.getHouseId();
        vo.roomId = o.getRoomId();
        vo.guestName = o.getGuestName();
        vo.phone = o.getPhone();
        vo.checkIn = o.getCheckIn();
        vo.checkOut = o.getCheckOut();
        vo.nights = o.getNights();
        vo.amount = o.getAmount();
        vo.status = o.getStatus();
        vo.remark = o.getRemark();
        vo.createTime = o.getCreateTime();
        vo.confirmTime = o.getConfirmTime();
        vo.checkInTime = o.getCheckInTime();
        vo.completeTime = o.getCompleteTime();
        vo.cancelReason = o.getCancelReason();
        switch (o.getStatus() == null ? "" : o.getStatus()) {
            case "待确认": vo.statusText = "商家确认中，请保持电话畅通"; break;
            case "已确认": vo.statusText = "预约成功，凭预订人手机号办理入住"; break;
            case "已入住": vo.statusText = "已办理入住，祝入住愉快"; break;
            case "已完成": vo.statusText = "订单已完成，感谢您的入住"; break;
            case "已取消": vo.statusText = "订单已取消"; break;
            default: vo.statusText = "";
        }
        return vo;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getHouseId() { return houseId; }
    public void setHouseId(Integer houseId) { this.houseId = houseId; }
    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }
    public String getHouseName() { return houseName; }
    public void setHouseName(String houseName) { this.houseName = houseName; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getConfirmTime() { return confirmTime; }
    public void setConfirmTime(String confirmTime) { this.confirmTime = confirmTime; }
    public String getCheckInTime() { return checkInTime; }
    public void setCheckInTime(String checkInTime) { this.checkInTime = checkInTime; }
    public String getCompleteTime() { return completeTime; }
    public void setCompleteTime(String completeTime) { this.completeTime = completeTime; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantPhone() { return merchantPhone; }
    public void setMerchantPhone(String merchantPhone) { this.merchantPhone = merchantPhone; }
}
