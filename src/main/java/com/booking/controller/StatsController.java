package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.config.AdminAuthInterceptor;
import com.booking.dto.HouseVO;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.BookingOrder;
import com.booking.entity.House;
import com.booking.mapper.BookingOrderMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家后台-经营统计（数据隔离：仅统计本商家数据）
 */
@RestController
@RequestMapping("/api/admin/stats")
public class StatsController {

    private final BookingOrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final JdbcTemplate jdbc;

    public StatsController(BookingOrderMapper orderMapper, HouseMapper houseMapper,
                           RoomMapper roomMapper, JdbcTemplate jdbc) {
        this.orderMapper = orderMapper;
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
        this.jdbc = jdbc;
    }

    private Integer merchantId(HttpServletRequest req) {
        Admin a = (Admin) req.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return a == null ? 0 : (a.getMerchantId() == null ? 0 : a.getMerchantId());
    }

    private String houseSql(Integer mid) {
        return "SELECT id FROM house WHERE merchant_id = " + mid;
    }

    /**
     * 经营总览
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(HttpServletRequest req) {
        int mid = merchantId(req);
        String houseCond = "house_id IN (" + houseSql(mid) + ")";
        Map<String, Object> data = new HashMap<>();

        data.put("todayRevenue", sumConfirmed(houseCond + " AND date(create_time) = date('now','localtime')"));
        data.put("todayOrders", countWhere(houseCond + " AND date(create_time) = date('now','localtime')"));
        data.put("monthRevenue", sumConfirmed(houseCond + " AND strftime('%Y-%m', create_time) = strftime('%Y-%m','now','localtime')"));
        data.put("monthOrders", countWhere(houseCond + " AND strftime('%Y-%m', create_time) = strftime('%Y-%m','now','localtime')"));

        data.put("totalHouses", houseMapper.selectCount(new QueryWrapper<House>().eq("merchant_id", mid)));
        data.put("totalRooms", totalRoomStock(mid));
        data.put("occupiedRooms", occupiedRoomCount(mid));
        data.put("occupancyRate", occupancyRate(mid));

        // 订单状态分布
        data.put("orderStatus", orderStatusStats(houseCond));

        List<BookingOrder> recent = orderMapper.selectList(
                new QueryWrapper<BookingOrder>().inSql("house_id", houseSql(mid)).orderByDesc("id").last("LIMIT 5"));
        data.put("recentOrders", recent.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", o.getOrderNo());
            House h = houseMapper.selectById(o.getHouseId());
            m.put("houseName", h != null ? h.getName() : "");
            m.put("amount", o.getAmount());
            m.put("status", o.getStatus());
            return m;
        }).toList());

        return Result.ok(data);
    }

    /**
     * 营收趋势（近 7/30 天，口径：已确认/已入住/已完成订单）
     */
    @GetMapping("/revenue")
    public Result<List<Map<String, Object>>> revenue(HttpServletRequest req, @RequestParam(defaultValue = "7") int range) {
        int mid = merchantId(req);
        int days = range == 30 ? 30 : 7;
        String sql = "SELECT substr(create_time,1,10) AS d, COALESCE(SUM(amount),0) AS amt " +
                "FROM booking_order " +
                "WHERE status IN ('已确认','已入住','已完成') AND house_id IN (" + houseSql(mid) + ") " +
                "AND date(create_time) >= date('now','localtime', '-' || ? || ' days') " +
                "GROUP BY d ORDER BY d";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, days);
        rows.forEach(r -> r.put("amount", ((Number) r.get("amt")).doubleValue()));
        return Result.ok(rows);
    }

    private BigDecimal sumConfirmed(String where) {
        try {
            String sql = "SELECT COALESCE(SUM(amount),0) FROM booking_order WHERE status IN ('已确认','已入住','已完成') AND " + where;
            Number n = jdbc.queryForObject(sql, Number.class);
            return n == null ? BigDecimal.ZERO : BigDecimal.valueOf(n.doubleValue());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /** 订单状态分布 */
    private Map<String, Object> orderStatusStats(String houseCond) {
        Map<String, Object> map = new HashMap<>();
        try {
            String sql = "SELECT status, COUNT(*) AS cnt FROM booking_order WHERE " + houseCond + " GROUP BY status";
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            for (Map<String, Object> r : rows) {
                map.put(String.valueOf(r.get("status")), ((Number) r.get("cnt")).longValue());
            }
        } catch (Exception ignored) {
        }
        map.putIfAbsent("待确认", 0L);
        map.putIfAbsent("已确认", 0L);
        map.putIfAbsent("已入住", 0L);
        map.putIfAbsent("已完成", 0L);
        map.putIfAbsent("已取消", 0L);
        return map;
    }

    private long countWhere(String where) {
        try {
            String sql = "SELECT COUNT(*) FROM booking_order WHERE " + where;
            Number n = jdbc.queryForObject(sql, Number.class);
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private int totalRoomStock(int mid) {
        try {
            String sql = "SELECT COALESCE(SUM(r.stock),0) FROM room r " +
                    "JOIN house h ON r.house_id = h.id WHERE h.merchant_id = " + mid;
            Number n = jdbc.queryForObject(sql, Number.class);
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /** 当前有有效订单（待确认/已确认，且日期覆盖今天）的房型数 */
    private int occupiedRoomCount(int mid) {
        try {
            String sql = "SELECT COUNT(DISTINCT room_id) FROM booking_order " +
                    "WHERE status IN ('待确认','已确认') " +
                    "AND house_id IN (" + houseSql(mid) + ") " +
                    "AND date(check_in) <= date('now','localtime') " +
                    "AND date(check_out) > date('now','localtime')";
            Number n = jdbc.queryForObject(sql, Number.class);
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private double occupancyRate(int mid) {
        int total = totalRoomStock(mid);
        if (total <= 0) return 0;
        return Math.round(occupiedRoomCount(mid) * 1000.0 / total) / 10.0;
    }
}
