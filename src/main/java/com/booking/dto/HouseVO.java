package com.booking.dto;

import com.booking.entity.House;
import com.booking.entity.Room;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 房源视图：列表 + 详情（含房型）
 */
public class HouseVO {

    private Integer id;
    private String name;
    private String type;
    private String area;
    private String address;
    private Double score;
    private Integer reviews;
    private BigDecimal price;
    private String description;
    private List<String> tags;
    private String imgUrl;
    private List<Room> rooms;

    /** 封面原始配置（真实图片路径 /uploads/.. 或 AI 提示词），编辑回填用 */
    private String imgPrompt;

    /** 状态：1=上架，0=下架（管理端展示用） */
    private Integer status;

    /** 归属商家 id */
    private Integer merchantId;

    /** 商家名称（C 端展示） */
    private String merchantName;

    /** 商家联系电话（C 端一键拨号） */
    private String merchantPhone;

    /** 商家门店地址（C 端展示/导航） */
    private String merchantAddress;

    private Double merchantLng;

    private Double merchantLat;

    /** 商家营业状态：1=营业中，0=暂停接单 */
    private Integer openStatus;

    public static HouseVO from(House h) {
        HouseVO vo = new HouseVO();
        vo.id = h.getId();
        vo.name = h.getName();
        vo.type = h.getType();
        vo.area = h.getArea();
        vo.address = h.getAddress();
        vo.score = h.getScore();
        vo.reviews = h.getReviews();
        vo.price = h.getPrice();
        vo.description = h.getDescription();
        vo.status = h.getStatus();
        vo.merchantId = h.getMerchantId();
        vo.tags = new ArrayList<>();
        if (h.getTags() != null) {
            for (String t : h.getTags().split(",")) {
                String s = t.trim();
                if (!s.isEmpty()) vo.tags.add(s);
            }
        }
        vo.imgUrl = imgUrl(h.getImgPrompt());
        vo.imgPrompt = h.getImgPrompt();
        return vo;
    }

    /** 图片地址：真实 URL（http/相对 /uploads）直接使用；否则按 prompt 生成 AI 占位图 */
    public static String imgUrl(String promptOrUrl) {
        if (promptOrUrl == null || promptOrUrl.isBlank()) return "";
        String v = promptOrUrl.trim();
        if (v.startsWith("http") || v.startsWith("/uploads/")) {
            return v;
        }
        String encoded = URLEncoder.encode(v, StandardCharsets.UTF_8);
        return "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt="
                + encoded + "&image_size=landscape_4_3";
    }

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
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getImgUrl() { return imgUrl; }
    public void setImgUrl(String imgUrl) { this.imgUrl = imgUrl; }
    public String getImgPrompt() { return imgPrompt; }
    public void setImgPrompt(String imgPrompt) { this.imgPrompt = imgPrompt; }
    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getMerchantId() { return merchantId; }
    public void setMerchantId(Integer merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getMerchantPhone() { return merchantPhone; }
    public void setMerchantPhone(String merchantPhone) { this.merchantPhone = merchantPhone; }
    public String getMerchantAddress() { return merchantAddress; }
    public void setMerchantAddress(String merchantAddress) { this.merchantAddress = merchantAddress; }
    public Double getMerchantLng() { return merchantLng; }
    public void setMerchantLng(Double merchantLng) { this.merchantLng = merchantLng; }
    public Double getMerchantLat() { return merchantLat; }
    public void setMerchantLat(Double merchantLat) { this.merchantLat = merchantLat; }
    public Integer getOpenStatus() { return openStatus; }
    public void setOpenStatus(Integer openStatus) { this.openStatus = openStatus; }
}
