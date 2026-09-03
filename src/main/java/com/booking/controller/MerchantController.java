package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.dto.HouseVO;
import com.booking.entity.Admin;
import com.booking.entity.House;
import com.booking.mapper.AdminMapper;
import com.booking.mapper.HouseMapper;
import com.booking.util.ImgUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家主页接口（C 端，公开）
 */
@RestController
@RequestMapping("/api/merchants")
@CrossOrigin
public class MerchantController {

    private final AdminMapper adminMapper;
    private final HouseMapper houseMapper;

    public MerchantController(AdminMapper adminMapper, HouseMapper houseMapper) {
        this.adminMapper = adminMapper;
        this.houseMapper = houseMapper;
    }

    /**
     * 商家主页：基本信息 + 在售房源
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id, HttpServletRequest req) {
        Admin m = adminMapper.selectById(id);
        if (m == null || !"merchant".equals(m.getRole())
                || m.getStatus() == null || m.getStatus() != 1) {
            return Map.of("code", 404, "msg", "商家不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", m.getId());
        data.put("merchantName", m.getMerchantName());
        data.put("description", m.getDescription());
        data.put("contactPhone", m.getContactPhone());
        data.put("openStatus", m.getOpenStatus() == null ? 1 : m.getOpenStatus());
        data.put("address", m.getAddress());
        data.put("lng", m.getLng());
        data.put("lat", m.getLat());

        // 商家在售房源
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("merchant_id", id).eq("status", 1).orderByDesc("id");
        List<House> houses = houseMapper.selectList(qw);
        List<HouseVO> voList = new ArrayList<>();
        for (House h : houses) {
            HouseVO vo = HouseVO.from(h);
            vo.setImgUrl(ImgUtil.absolute(req, vo.getImgUrl()));
            vo.setMerchantName(m.getMerchantName());
            vo.setOpenStatus(m.getOpenStatus());
            voList.add(vo);
        }
        data.put("houses", voList);
        data.put("houseCount", voList.size());

        return Map.of("code", 200, "msg", "ok", "data", data);
    }
}
