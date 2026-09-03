package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.config.AdminAuthInterceptor;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.BookingOrder;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.BookingOrderMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商家后台-房型管理（数据隔离：仅操作本商家房源下的房型）
 */
@RestController
@RequestMapping("/api/admin")
public class AdminRoomController {

    private final RoomMapper roomMapper;
    private final BookingOrderMapper orderMapper;
    private final HouseMapper houseMapper;

    public AdminRoomController(RoomMapper roomMapper, BookingOrderMapper orderMapper, HouseMapper houseMapper) {
        this.roomMapper = roomMapper;
        this.orderMapper = orderMapper;
        this.houseMapper = houseMapper;
    }

    private Integer merchantId(HttpServletRequest req) {
        Admin a = (Admin) req.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return a == null ? 0 : (a.getMerchantId() == null ? 0 : a.getMerchantId());
    }

    /** 当前商家名下的房源是否存在 */
    private boolean ownedHouse(HttpServletRequest req, Integer houseId) {
        return houseMapper.selectCount(new QueryWrapper<House>()
                .eq("id", houseId).eq("merchant_id", merchantId(req))) > 0;
    }

    /** 当前商家名下的房型，无归属返回 null */
    private Room ownedRoom(HttpServletRequest req, Integer roomId) {
        Room r = roomMapper.selectById(roomId);
        if (r == null) return null;
        return ownedHouse(req, r.getHouseId()) ? r : null;
    }

    /**
     * 某房源的房型列表
     */
    @GetMapping("/houses/{houseId}/rooms")
    public Result<List<Room>> list(HttpServletRequest req, @PathVariable Integer houseId) {
        if (!ownedHouse(req, houseId)) return Result.error(404, "房源不存在");
        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.eq("house_id", houseId).orderByAsc("price");
        return Result.ok(roomMapper.selectList(qw));
    }

    /**
     * 新增房型
     */
    @PostMapping("/houses/{houseId}/rooms")
    public Result<Room> create(HttpServletRequest req, @PathVariable Integer houseId, @RequestBody Room room) {
        if (!ownedHouse(req, houseId)) return Result.error(404, "房源不存在");
        if (room.getName() == null || room.getName().isBlank()) {
            return Result.error(400, "请填写房型名称");
        }
        if (room.getPrice() == null) room.setPrice(java.math.BigDecimal.ZERO);
        if (room.getStock() == null) room.setStock(0);
        room.setId(null);
        room.setHouseId(houseId);
        roomMapper.insert(room);
        return Result.ok(room);
    }

    /**
     * 修改房型
     */
    @PutMapping("/rooms/{id}")
    public Result<Void> update(HttpServletRequest req, @PathVariable Integer id, @RequestBody Room room) {
        if (ownedRoom(req, id) == null) return Result.error(404, "房型不存在");
        room.setId(id);
        roomMapper.updateById(room);
        return Result.ok();
    }

    /**
     * 删除房型（存在未取消订单时拒绝）
     */
    @DeleteMapping("/rooms/{id}")
    public Result<Void> delete(HttpServletRequest req, @PathVariable Integer id) {
        if (ownedRoom(req, id) == null) return Result.error(404, "房型不存在");
        Long active = orderMapper.selectCount(
                new QueryWrapper<BookingOrder>().eq("room_id", id).ne("status", "已取消"));
        if (active != null && active > 0) {
            return Result.error(400, "该房型存在进行中的订单，无法删除");
        }
        roomMapper.deleteById(id);
        return Result.ok();
    }
}
