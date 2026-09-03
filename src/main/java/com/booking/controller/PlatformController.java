package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booking.config.AdminAuthInterceptor;
import com.booking.config.TokenService;
import com.booking.dto.HouseVO;
import com.booking.dto.OrderVO;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.BookingOrder;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.AdminMapper;
import com.booking.mapper.BookingOrderMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台方（开发者）管理接口：商家入驻审核、商家管理、平台总览、查看全部数据
 */
@RestController
@RequestMapping("/api/platform")
public class PlatformController {

    private final AdminMapper adminMapper;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final BookingOrderMapper orderMapper;
    private final JdbcTemplate jdbc;
    private final TokenService tokenService;

    public PlatformController(AdminMapper adminMapper, HouseMapper houseMapper,
                              RoomMapper roomMapper, BookingOrderMapper orderMapper,
                              JdbcTemplate jdbc, TokenService tokenService) {
        this.adminMapper = adminMapper;
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
        this.orderMapper = orderMapper;
        this.jdbc = jdbc;
        this.tokenService = tokenService;
    }

    // ================= 商家入驻审核 =================

    /** 入驻申请列表 */
    @GetMapping("/applies")
    public Result<Map<String, Object>> applies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String applyStatus) {
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.eq("role", "merchant");
        if (applyStatus != null && !applyStatus.isBlank()) {
            qw.eq("apply_status", applyStatus);
        }
        qw.orderByDesc("id");
        Page<Admin> p = adminMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(this::merchantVO).toList());
        return Result.ok(data);
    }

    /** 审核通过（分配商家身份并启用） */
    @PostMapping("/applies/{id}/approve")
    public Result<Void> approve(@PathVariable Integer id) {
        Admin a = adminMapper.selectById(id);
        if (a == null) return Result.error(404, "申请不存在");
        a.setMerchantId(a.getId());
        a.setApplyStatus("通过");
        a.setRejectReason(null);
        a.setStatus(1);
        // 注意：不覆写 nickname —— 入驻申请时 nickname 存的是"联系人"，
        // 审核通过后仍需在商家管理中展示真实联系人
        adminMapper.updateById(a);
        return Result.ok();
    }

    /** 审核拒绝 */
    @PostMapping("/applies/{id}/reject")
    public Result<Void> reject(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        Admin a = adminMapper.selectById(id);
        if (a == null) return Result.error(404, "申请不存在");
        a.setApplyStatus("拒绝");
        a.setRejectReason(body != null ? body.getOrDefault("reason", "") : "");
        a.setStatus(0);
        adminMapper.updateById(a);
        return Result.ok();
    }

    /** 恢复被拒申请为待审核（供平台误拒修正，或配合商家重新提交） */
    @PostMapping("/applies/{id}/pending")
    public Result<Void> revertPending(@PathVariable Integer id) {
        Admin a = adminMapper.selectById(id);
        if (a == null) return Result.error(404, "申请不存在");
        if (!"拒绝".equals(a.getApplyStatus())) {
            return Result.error(400, "仅被拒绝的申请可恢复待审核");
        }
        a.setApplyStatus("申请中");
        a.setRejectReason(null);
        a.setStatus(0);
        adminMapper.updateById(a);
        return Result.ok();
    }

    // ================= 商家管理 =================

    /** 商家列表 */
    @GetMapping("/merchants")
    public Result<Map<String, Object>> merchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.eq("role", "merchant").eq("apply_status", "通过");
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like("merchant_name", keyword.trim()).or().like("contact_phone", keyword.trim()));
        }
        qw.orderByDesc("id");
        Page<Admin> p = adminMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(this::merchantVO).toList());
        return Result.ok(data);
    }

    /** 启用/停用商家账号 */
    @PutMapping("/merchants/{id}/status")
    public Result<Void> merchantStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Admin a = adminMapper.selectById(id);
        if (a == null) return Result.error(404, "商家不存在");
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0 或 1");
        }
        a.setStatus(status);
        adminMapper.updateById(a);
        return Result.ok();
    }

    /** 平台管控商家营业状态：1=营业，0=暂停接单 */
    @PutMapping("/merchants/{id}/open-status")
    public Result<Void> merchantOpenStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Admin a = adminMapper.selectById(id);
        if (a == null) return Result.error(404, "商家不存在");
        Integer open = body.get("openStatus");
        if (open == null || (open != 0 && open != 1)) {
            return Result.error(400, "openStatus 必须为 0 或 1");
        }
        a.setOpenStatus(open);
        adminMapper.updateById(a);
        return Result.ok();
    }

    /** 平台重置商家登录密码（改密后该商家需重新登录） */
    @PutMapping("/merchants/{id}/password")
    public Result<Void> merchantPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Admin a = adminMapper.selectById(id);
        if (a == null || !"merchant".equals(a.getRole())) return Result.error(404, "商家不存在");
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) {
            return Result.error(400, "新密码至少 6 位");
        }
        a.setPassword(newPwd.trim());
        adminMapper.updateById(a);
        // 强制该商家所有已登录会话下线
        tokenService.removeByUsername(a.getUsername());
        return Result.ok();
    }

    /** 商家详情（资料 + 名下房源/订单统计 + 最近订单） */
    @GetMapping("/merchants/{id}")
    public Result<Map<String, Object>> merchantDetail(@PathVariable Integer id) {
        Admin a = adminMapper.selectById(id);
        if (a == null || !"merchant".equals(a.getRole())) return Result.error(404, "商家不存在");
        Map<String, Object> data = merchantVO(a);
        data.put("description", a.getDescription());
        data.put("openStatus", a.getOpenStatus());
        data.put("rejectReason", a.getRejectReason());

        String houseSql = "SELECT id FROM house WHERE merchant_id = " + a.getId();
        Long orderTotal = orderMapper.selectCount(new QueryWrapper<BookingOrder>().inSql("house_id", houseSql));
        data.put("orderCount", orderTotal == null ? 0 : orderTotal);
        data.put("revenue", sumConfirmed("house_id IN (" + houseSql + ")"));

        // 最近订单
        List<BookingOrder> recent = orderMapper.selectList(
                new QueryWrapper<BookingOrder>().inSql("house_id", houseSql).orderByDesc("id").last("LIMIT 5"));
        data.put("recentOrders", recent.stream().map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", o.getOrderNo());
            House h = houseMapper.selectById(o.getHouseId());
            m.put("houseName", h != null ? h.getName() : "");
            m.put("amount", o.getAmount());
            m.put("status", o.getStatus());
            m.put("createTime", o.getCreateTime());
            return m;
        }).toList());
        return Result.ok(data);
    }

    /** 平台开发者当前信息 */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        Admin admin = (Admin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        Map<String, Object> data = new HashMap<>();
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("role", admin.getRole());
        return Result.ok(data);
    }

    /** 平台开发者修改密码 */
    @PutMapping("/password")
    public Result<Void> password(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Admin admin = (Admin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || !oldPwd.equals(admin.getPassword())) {
            return Result.error(400, "原密码不正确");
        }
        if (newPwd == null || newPwd.length() < 6) {
            return Result.error(400, "新密码至少 6 位");
        }
        admin.setPassword(newPwd);
        adminMapper.updateById(admin);
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.remove(auth.substring(7));
        }
        return Result.ok();
    }

    // ================= 平台总览（所有商家数据） =================

    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> statsOverview() {
        Map<String, Object> data = new HashMap<>();
        data.put("merchantCount", count("SELECT COUNT(*) FROM admin WHERE role='merchant' AND apply_status='通过'"));
        data.put("applyPending", count("SELECT COUNT(*) FROM admin WHERE role='merchant' AND apply_status='申请中'"));
        data.put("pausedMerchants", count("SELECT COUNT(*) FROM admin WHERE role='merchant' AND apply_status='通过' AND open_status=0"));
        data.put("houseCount", count("SELECT COUNT(*) FROM house"));
        data.put("onHouseCount", count("SELECT COUNT(*) FROM house WHERE status=1"));
        data.put("orderCount", count("SELECT COUNT(*) FROM booking_order"));
        data.put("todayRevenue", sumConfirmed("date(create_time) = date('now','localtime')"));
        data.put("monthRevenue", sumConfirmed("strftime('%Y-%m', create_time) = strftime('%Y-%m','now','localtime')"));
        data.put("monthOrders", count("SELECT COUNT(*) FROM booking_order WHERE strftime('%Y-%m', create_time) = strftime('%Y-%m','now','localtime')"));
        data.put("roomStock", count("SELECT COALESCE(SUM(stock),0) FROM room"));
        data.put("occupiedRooms", count("SELECT COUNT(DISTINCT room_id) FROM booking_order WHERE status IN ('待确认','已确认') AND date(check_in) <= date('now','localtime') AND date(check_out) > date('now','localtime')"));
        data.put("orderStatus", orderStatusStats());
        data.put("recentApplies", recentApplies());
        return Result.ok(data);
    }

    /** 平台营收趋势（所有商家，口径：有效订单） */
    @GetMapping("/stats/revenue")
    public Result<List<Map<String, Object>>> statsRevenue(@RequestParam(defaultValue = "7") int range) {
        int days = range == 30 ? 30 : 7;
        String sql = "SELECT substr(create_time,1,10) AS d, COALESCE(SUM(amount),0) AS amt FROM booking_order " +
                "WHERE status IN ('已确认','已入住','已完成') AND date(create_time) >= date('now','localtime', '-' || ? || ' days') " +
                "GROUP BY d ORDER BY d";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, days);
        rows.forEach(r -> r.put("amount", ((Number) r.get("amt")).doubleValue()));
        return Result.ok(rows);
    }

    /** 全平台订单状态分布 */
    private Map<String, Object> orderStatusStats() {
        Map<String, Object> map = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT status, COUNT(*) AS cnt FROM booking_order GROUP BY status");
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

    /** 近期入驻申请动态 */
    private List<Map<String, Object>> recentApplies() {
        List<Admin> list = adminMapper.selectList(
                new QueryWrapper<Admin>().eq("role", "merchant").orderByDesc("id").last("LIMIT 5"));
        return list.stream().map(this::merchantVO).toList();
    }

    // ================= 查看全部数据（平台） =================

    /** 全部订单（可按商家/状态筛选） */
    @GetMapping("/orders")
    public Result<Map<String, Object>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer merchantId,
            @RequestParam(required = false) String status) {
        QueryWrapper<BookingOrder> qw = new QueryWrapper<>();
        if (merchantId != null) qw.inSql("house_id", "SELECT id FROM house WHERE merchant_id = " + merchantId);
        if (status != null && !status.isBlank() && !"全部".equals(status)) qw.eq("status", status);
        qw.orderByDesc("id");
        Page<BookingOrder> p = orderMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(o -> {
            OrderVO vo = OrderVO.from(o);
            House h = houseMapper.selectById(o.getHouseId());
            if (h != null) {
                vo.setHouseName(h.getName());
                vo.setImgUrl(HouseVO.imgUrl(h.getImgPrompt()));
                Admin m = adminMapper.selectById(h.getMerchantId());
                vo.setRemark("商家：" + (m != null && m.getMerchantName() != null ? m.getMerchantName() : "未知"));
            }
            Room r = roomMapper.selectById(o.getRoomId());
            if (r != null) vo.setRoomName(r.getName());
            return vo;
        }).toList());
        return Result.ok(data);
    }

    /** 全部房源（可按商家/类型/状态筛选） */
    @GetMapping("/houses")
    public Result<Map<String, Object>> houses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer merchantId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        if (merchantId != null) qw.eq("merchant_id", merchantId);
        if (type != null && !type.isBlank() && !"全部".equals(type)) qw.eq("type", type);
        if (status != null) qw.eq("status", status);
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            qw.and(w -> w.like("name", k).or().like("area", k).or().like("address", k).or().like("tags", k));
        }
        qw.orderByDesc("id");
        Page<House> p = houseMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(h -> {
            HouseVO vo = HouseVO.from(h);
            Admin m = adminMapper.selectById(h.getMerchantId());
            Map<String, Object> m2 = new HashMap<>();
            m2.put("id", vo.getId());
            m2.put("name", vo.getName());
            m2.put("type", vo.getType());
            m2.put("area", vo.getArea());
            m2.put("price", vo.getPrice());
            m2.put("status", vo.getStatus());
            m2.put("imgUrl", vo.getImgUrl());
            m2.put("merchantName", m != null ? m.getMerchantName() : "未知");
            return m2;
        }).toList());
        return Result.ok(data);
    }

    /** 上架/下架房源（影响 C 端展示） */
    @PutMapping("/houses/{id}/status")
    public Result<Void> houseStatus(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        House h = houseMapper.selectById(id);
        if (h == null) return Result.error(404, "房源不存在");
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0 或 1");
        }
        h.setStatus(status);
        houseMapper.updateById(h);
        return Result.ok();
    }

    /** 平台替换房源封面图（商家未上传真图时平台可兜底处理） */
    @PutMapping("/houses/{id}/img")
    public Result<Void> houseImg(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        House h = houseMapper.selectById(id);
        if (h == null) return Result.error(404, "房源不存在");
        String img = body.get("imgPrompt");
        h.setImgPrompt(img == null ? "" : img.trim());
        houseMapper.updateById(h);
        return Result.ok();
    }

    // ================= 辅助 =================

    private Map<String, Object> merchantVO(Admin a) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("username", a.getUsername());
        m.put("merchantName", a.getMerchantName());
        m.put("contactName", a.getNickname());
        m.put("contactPhone", a.getContactPhone());
        m.put("applyStatus", a.getApplyStatus());
        m.put("rejectReason", a.getRejectReason());
        m.put("status", a.getStatus());
        m.put("openStatus", a.getOpenStatus());
        m.put("address", a.getAddress());
        m.put("lng", a.getLng());
        m.put("lat", a.getLat());
        m.put("createTime", a.getCreateTime());
        Long houseCount = houseMapper.selectCount(new QueryWrapper<House>().eq("merchant_id", a.getId()));
        m.put("houseCount", houseCount == null ? 0 : houseCount);
        return m;
    }

    private long count(String sql) {
        try {
            Number n = jdbc.queryForObject(sql, Number.class);
            return n == null ? 0 : n.longValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private java.math.BigDecimal sumConfirmed(String where) {
        try {
            Number n = jdbc.queryForObject(
                    "SELECT COALESCE(SUM(amount),0) FROM booking_order WHERE status IN ('已确认','已入住','已完成') AND " + where, Number.class);
            return n == null ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(n.doubleValue());
        } catch (Exception e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
