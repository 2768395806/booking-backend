package com.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 首页轮播图
 */
@TableName("banner")
public class Banner {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联房源 id（点击跳转详情） */
    private Integer houseId;

    /** 图片地址（/uploads/... 或 http...） */
    private String imageUrl;

    /** 标题 */
    private String title;

    /** 排序值（越小越靠前） */
    private Integer sort;

    /** 1=显示 0=隐藏 */
    private Integer status;

    private String createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getHouseId() { return houseId; }
    public void setHouseId(Integer houseId) { this.houseId = houseId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }
}
