package com.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 房源
 */
@TableName("house")
public class House {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    /** 类型：民宿 / 酒店 */
    private String type;

    /** 区域 */
    private String area;

    private String address;

    private Double score;

    private Integer reviews;

    /** 起价 */
    private BigDecimal price;

    private String description;

    /** 标签，逗号分隔 */
    private String tags;

    /** 图片 prompt（原型占位，后续换真实图片 URL） */
    private String imgPrompt;

    /** 状态：1=上架，0=下架 */
    private Integer status;

    /** 归属商家 id（多商家数据隔离） */
    private Integer merchantId;

    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public Integer getReviews() { return reviews; }
    public void setReviews(Integer reviews) { this.reviews = reviews; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getImgPrompt() { return imgPrompt; }
    public void setImgPrompt(String imgPrompt) { this.imgPrompt = imgPrompt; }
}
