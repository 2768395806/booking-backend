package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.dto.Result;
import com.booking.entity.Banner;
import com.booking.entity.House;
import com.booking.mapper.BannerMapper;
import com.booking.mapper.HouseMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页轮播图管理接口（平台开发者专属）
 * /api/platform/** 由 AdminAuthInterceptor 校验 platform 角色，
 * 商家账号无法访问，满足"只有开发者能添加民宿图片"。
 */
@RestController
@RequestMapping("/api/platform/banners")
public class PlatformBannerController {

    private final BannerMapper bannerMapper;
    private final HouseMapper houseMapper;

    public PlatformBannerController(BannerMapper bannerMapper, HouseMapper houseMapper) {
        this.bannerMapper = bannerMapper;
        this.houseMapper = houseMapper;
    }

    /** 轮播列表（含全部状态） */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Banner> banners = bannerMapper.selectList(
                new QueryWrapper<Banner>().orderByAsc("sort").orderByAsc("id"));
        return Result.ok(banners.stream().map(this::vo).toList());
    }

    /** 新增轮播 */
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Integer houseId = asInt(body.get("houseId"));
        if (houseId == null || houseMapper.selectById(houseId) == null) {
            return Result.error(400, "请选择有效的房源");
        }
        String imageUrl = body.get("imageUrl") == null ? "" : String.valueOf(body.get("imageUrl")).trim();
        if (imageUrl.isEmpty()) {
            return Result.error(400, "请上传轮播图片");
        }
        Banner b = new Banner();
        b.setHouseId(houseId);
        b.setImageUrl(imageUrl);
        b.setTitle(body.get("title") == null ? "" : String.valueOf(body.get("title")).trim());
        b.setSort(body.get("sort") == null ? 0 : asInt(body.get("sort")));
        b.setStatus(1);
        b.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        bannerMapper.insert(b);
        return Result.ok();
    }

    /** 修改轮播 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) return Result.error(404, "轮播不存在");
        Integer houseId = asInt(body.get("houseId"));
        if (houseId != null) {
            if (houseMapper.selectById(houseId) == null) return Result.error(400, "请选择有效的房源");
            b.setHouseId(houseId);
        }
        if (body.get("imageUrl") != null && !String.valueOf(body.get("imageUrl")).trim().isEmpty()) {
            b.setImageUrl(String.valueOf(body.get("imageUrl")).trim());
        }
        if (body.get("title") != null) b.setTitle(String.valueOf(body.get("title")).trim());
        if (body.get("sort") != null) b.setSort(asInt(body.get("sort")));
        bannerMapper.updateById(b);
        return Result.ok();
    }

    /** 上架/下架 */
    @PutMapping("/{id}/status")
    public Result<Void> status(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Banner b = bannerMapper.selectById(id);
        if (b == null) return Result.error(404, "轮播不存在");
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) return Result.error(400, "status 必须为 0 或 1");
        b.setStatus(status);
        bannerMapper.updateById(b);
        return Result.ok();
    }

    /** 删除轮播 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        if (bannerMapper.selectById(id) == null) return Result.error(404, "轮播不存在");
        bannerMapper.deleteById(id);
        return Result.ok();
    }

    /** 可选择关联的房源下拉数据（仅上架房源） */
    @GetMapping("/houses")
    public Result<List<Map<String, Object>>> houseOptions() {
        List<House> houses = houseMapper.selectList(
                new QueryWrapper<House>().eq("status", 1).orderByDesc("id"));
        return Result.ok(houses.stream().map(h -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", h.getId());
            m.put("name", h.getName());
            return m;
        }).toList());
    }

    private Map<String, Object> vo(Banner b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("houseId", b.getHouseId());
        m.put("imageUrl", b.getImageUrl());
        m.put("title", b.getTitle());
        m.put("sort", b.getSort());
        m.put("status", b.getStatus());
        m.put("createTime", b.getCreateTime());
        House h = houseMapper.selectById(b.getHouseId());
        m.put("houseName", h != null ? h.getName() : "房源已删除");
        return m;
    }

    private Integer asInt(Object v) {
        if (v == null) return null;
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
