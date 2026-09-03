package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.entity.Admin;
import com.booking.entity.Banner;
import com.booking.entity.House;
import com.booking.mapper.AdminMapper;
import com.booking.mapper.BannerMapper;
import com.booking.mapper.HouseMapper;
import com.booking.util.ImgUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页轮播图接口（C 端小程序，无需登录）
 */
@RestController
@RequestMapping("/api/banners")
@CrossOrigin
public class BannerController {

    private final BannerMapper bannerMapper;
    private final HouseMapper houseMapper;
    private final AdminMapper adminMapper;

    public BannerController(BannerMapper bannerMapper, HouseMapper houseMapper, AdminMapper adminMapper) {
        this.bannerMapper = bannerMapper;
        this.houseMapper = houseMapper;
        this.adminMapper = adminMapper;
    }

    /**
     * 展示中的轮播列表（按 sort 升序）
     * 返回含关联房源信息（houseId/houseName），点击可直接跳转房源详情
     */
    @GetMapping
    public List<Map<String, Object>> list(HttpServletRequest req) {
        QueryWrapper<Banner> qw = new QueryWrapper<>();
        qw.eq("status", 1).orderByAsc("sort").orderByAsc("id");
        List<Banner> banners = bannerMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Banner b : banners) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("houseId", b.getHouseId());
            m.put("imageUrl", ImgUtil.absolute(req, b.getImageUrl()));
            m.put("title", b.getTitle());
            House h = houseMapper.selectById(b.getHouseId());
            if (h != null) {
                m.put("houseName", h.getName());
                Admin a = adminMapper.selectById(h.getMerchantId());
                m.put("merchantName", a != null && a.getMerchantName() != null ? a.getMerchantName() : "");
            } else {
                m.put("houseName", "");
                m.put("merchantName", "");
            }
            result.add(m);
        }
        return result;
    }
}
