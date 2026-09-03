package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booking.config.AdminAuthInterceptor;
import com.booking.dto.HouseVO;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家后台-房源管理
 */
@RestController
@RequestMapping("/api/admin/houses")
public class AdminHouseController {

    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;

    public AdminHouseController(HouseMapper houseMapper, RoomMapper roomMapper) {
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
    }

    private Integer merchantId(HttpServletRequest req) {
        Admin a = (Admin) req.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return a == null ? 0 : (a.getMerchantId() == null ? 0 : a.getMerchantId());
    }

    /**
     * 房源列表（分页）
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {

        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("merchant_id", merchantId(req));   // 数据隔离：仅当前商家房源
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("name", keyword.trim()).or().like("area", keyword.trim()));
        }
        if (type != null && !type.isBlank()) {
            qw.eq("type", type);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");

        Page<House> p = houseMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(HouseVO::from).toList());
        return Result.ok(data);
    }

    /**
     * 房源详情（含房型）
     */
    @GetMapping("/{id}")
    public Result<HouseVO> detail(HttpServletRequest req, @PathVariable Integer id) {
        House h = ownedHouse(req, id);
        if (h == null) return Result.error(404, "房源不存在");
        HouseVO vo = HouseVO.from(h);
        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.eq("house_id", id).orderByAsc("price");
        vo.setRooms(roomMapper.selectList(qw));
        return Result.ok(vo);
    }

    /**
     * 新增房源
     */
    @PostMapping
    public Result<HouseVO> create(HttpServletRequest req, @RequestBody House h) {
        if (h.getName() == null || h.getName().isBlank()) {
            return Result.error(400, "请填写房源名称");
        }
        if (h.getStatus() == null) h.setStatus(1);
        if (h.getScore() == null) h.setScore(0.0);
        if (h.getReviews() == null) h.setReviews(0);
        h.setMerchantId(merchantId(req));   // 房源归属当前商家
        houseMapper.insert(h);
        return Result.ok(HouseVO.from(h));
    }

    /**
     * 修改房源
     */
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest req, @PathVariable Integer id, @RequestBody House h) {
        if (ownedHouse(req, id) == null) return Result.error(404, "房源不存在");
        h.setId(id);
        h.setMerchantId(merchantId(req));   // 防止越权改归属
        houseMapper.updateById(h);
        return Result.ok();
    }

    /**
     * 删除房源（存在房型时拒绝）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest req, @PathVariable Integer id) {
        if (ownedHouse(req, id) == null) return Result.error(404, "房源不存在");
        Long roomCount = roomMapper.selectCount(new QueryWrapper<Room>().eq("house_id", id));
        if (roomCount != null && roomCount > 0) {
            return Result.error(400, "该房源下存在房型，请先删除房型");
        }
        houseMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 上架/下架
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(HttpServletRequest req, @PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        House h = ownedHouse(req, id);
        if (h == null) return Result.error(404, "房源不存在");
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0 或 1");
        }
        h.setStatus(status);
        houseMapper.updateById(h);
        return Result.ok();
    }

    /** 查询当前商家名下的房源，无归属返回 null */
    private House ownedHouse(HttpServletRequest req, Integer id) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("merchant_id", merchantId(req));
        return houseMapper.selectOne(qw);
    }
}
