package com.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 房源评论
 */
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer houseId;

    private String guestName;

    private String phone;

    /** 评分 1-5 */
    private Integer rating;

    private String content;

    /** 商家回复 */
    private String reply;

    /** 状态：1=显示，0=隐藏 */
    private Integer status;

    private String createTime;

    private String replyTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getHouseId() { return houseId; }
    public void setHouseId(Integer houseId) { this.houseId = houseId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
    public String getReplyTime() { return replyTime; }
    public void setReplyTime(String replyTime) { this.replyTime = replyTime; }
}
