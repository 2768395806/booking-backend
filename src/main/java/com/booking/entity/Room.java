package com.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/**
 * 房型
 */
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer houseId;

    private String name;

    private BigDecimal price;

    /** 剩余可订数量 */
    private Integer stock;

    /** 房型图片：真实 URL（http/相对 /uploads）或 AI prompt */
    private String img;

    /** 输出用：补全为绝对地址的房型图（非数据库列） */
    @TableField(exist = false)
    private String imgUrl;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getHouseId() { return houseId; }
    public void setHouseId(Integer houseId) { this.houseId = houseId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImg() { return img; }
    public void setImg(String img) { this.img = img; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
}
