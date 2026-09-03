package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.dto.HouseVO;
import com.booking.entity.Favorite;
import com.booking.entity.House;
import com.booking.mapper.FavoriteMapper;
import com.booking.mapper.HouseMapper;
import com.booking.util.ImgUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户收藏（微信 openid 标识，无需登录态）
 */
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;
    private final HouseMapper houseMapper;

    public FavoriteController(FavoriteMapper favoriteMapper, HouseMapper houseMapper) {
        this.favoriteMapper = favoriteMapper;
        this.houseMapper = houseMapper;
    }

    /** 切换收藏：已收藏则取消，未收藏则添加；返回 favorited */
    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestBody Map<String, Object> body) {
        Object hid = body.get("houseId");
        String openid = body.get("openid") == null ? "" : String.valueOf(body.get("openid")).trim();
        Integer houseId = (hid instanceof Number n) ? n.intValue()
                : (hid != null ? Integer.valueOf(String.valueOf(hid)) : null);
        if (houseId == null || openid.isBlank()) {
            return Map.of("code", 400, "msg", "参数不完整，请先微信登录");
        }
        QueryWrapper<Favorite> qw = new QueryWrapper<>();
        qw.eq("house_id", houseId).eq("openid", openid);
        Favorite f = favoriteMapper.selectOne(qw);
        if (f != null) {
            favoriteMapper.deleteById(f.getId());
            return Map.of("code", 200, "favorited", false);
        }
        Favorite nf = new Favorite();
        nf.setHouseId(houseId);
        nf.setOpenid(openid);
        nf.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        favoriteMapper.insert(nf);
        return Map.of("code", 200, "favorited", true);
    }

    /** 是否已收藏 */
    @GetMapping("/status")
    public Map<String, Object> status(@RequestParam Integer houseId, @RequestParam(required = false) String openid) {
        if (openid == null || openid.isBlank()) {
            return Map.of("code", 200, "favorited", false);
        }
        QueryWrapper<Favorite> qw = new QueryWrapper<>();
        qw.eq("house_id", houseId).eq("openid", openid.trim());
        return Map.of("code", 200, "favorited", favoriteMapper.selectCount(qw) > 0);
    }

    /** 查询我的收藏（房源信息 + 收藏时间，按收藏倒序） */
    @GetMapping
    public List<Map<String, Object>> list(@RequestParam String openid, HttpServletRequest req) {
        QueryWrapper<Favorite> qw = new QueryWrapper<>();
        qw.eq("openid", openid == null ? "" : openid.trim()).orderByDesc("id");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite f : favoriteMapper.selectList(qw)) {
            House h = houseMapper.selectById(f.getHouseId());
            if (h == null || (h.getStatus() != null && h.getStatus() == 0)) continue;  // 已下架不展示
            HouseVO vo = HouseVO.from(h);
            vo.setImgUrl(ImgUtil.absolute(req, vo.getImgUrl()));
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("houseId", h.getId());
            m.put("name", vo.getName());
            m.put("type", vo.getType());
            m.put("area", vo.getArea());
            m.put("price", vo.getPrice());
            m.put("score", vo.getScore());
            m.put("imgUrl", vo.getImgUrl());
            m.put("favoriteTime", f.getCreateTime());
            result.add(m);
        }
        return result;
    }
}
