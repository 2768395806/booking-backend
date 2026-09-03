package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booking.config.AdminAuthInterceptor;
import com.booking.dto.OrderVO;
import com.booking.dto.HouseVO;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.BookingOrder;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.BookingOrderMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家后台-订单管理
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final BookingOrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;

    public AdminOrderController(BookingOrderMapper orderMapper, HouseMapper houseMapper, RoomMapper roomMapper) {
        this.orderMapper = orderMapper;
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
    }

    private Integer merchantId(HttpServletRequest req) {
        Admin a = (Admin) req.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return a == null ? 0 : (a.getMerchantId() == null ? 0 : a.getMerchantId());
    }

    private String merchantSql(Integer mid) {
        return "SELECT id FROM house WHERE merchant_id = " + mid;
    }

    /** 当前商家名下的订单，无归属返回 null */
    private BookingOrder ownedOrder(HttpServletRequest req, Integer id) {
        QueryWrapper<BookingOrder> qw = new QueryWrapper<>();
        qw.eq("id", id).inSql("house_id", merchantSql(merchantId(req)));
        return orderMapper.selectOne(qw);
    }

    /**
     * 订单列表（分页 + 筛选）
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        QueryWrapper<BookingOrder> qw = new QueryWrapper<>();
        qw.inSql("house_id", merchantSql(merchantId(req)));   // 数据隔离：仅本商家订单
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            qw.eq("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            qw.and(w -> w.like("order_no", k).or().like("guest_name", k).or().like("phone", k));
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            qw.ge("create_time", dateFrom.trim());
        }
        if (dateTo != null && !dateTo.isBlank()) {
            qw.le("create_time", dateTo.trim() + " 23:59:59");
        }
        qw.orderByDesc("id");

        Page<BookingOrder> p = orderMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", p.getRecords().stream().map(this::toVO).toList());
        return Result.ok(data);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(HttpServletRequest req, @PathVariable Integer id) {
        BookingOrder order = ownedOrder(req, id);
        if (order == null) return Result.error(404, "订单不存在");
        return Result.ok(toVO(order));
    }

    /**
     * 确认订单
     */
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(HttpServletRequest req, @PathVariable Integer id) {
        BookingOrder order = ownedOrder(req, id);
        if (order == null) return Result.error(404, "订单不存在");
        if (!"待确认".equals(order.getStatus())) {
            return Result.error(400, "当前状态不可确认（仅待确认订单可确认）");
        }
        order.setStatus("已确认");
        order.setConfirmTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 确认入住（已确认 → 已入住）
     */
    @PostMapping("/{id}/checkin")
    public Result<Void> checkin(HttpServletRequest req, @PathVariable Integer id) {
        BookingOrder order = ownedOrder(req, id);
        if (order == null) return Result.error(404, "订单不存在");
        if (!"已确认".equals(order.getStatus())) {
            return Result.error(400, "仅已确认订单可办理入住");
        }
        order.setStatus("已入住");
        order.setCheckInTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 完成订单（已入住 → 已完成）
     */
    @PostMapping("/{id}/complete")
    public Result<Void> complete(HttpServletRequest req, @PathVariable Integer id) {
        BookingOrder order = ownedOrder(req, id);
        if (order == null) return Result.error(404, "订单不存在");
        if (!"已入住".equals(order.getStatus())) {
            return Result.error(400, "仅已入住订单可完成");
        }
        order.setStatus("已完成");
        order.setCompleteTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 取消订单（商家侧）
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(HttpServletRequest req, @PathVariable Integer id,
                               @RequestBody(required = false) Map<String, String> body) {
        BookingOrder order = ownedOrder(req, id);
        if (order == null) return Result.error(404, "订单不存在");
        if ("已取消".equals(order.getStatus())) return Result.ok();
        order.setStatus("已取消");
        order.setCancelReason(body != null ? body.getOrDefault("reason", "") : "");
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 导出 CSV
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            HttpServletRequest req,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        QueryWrapper<BookingOrder> qw = new QueryWrapper<>();
        qw.inSql("house_id", merchantSql(merchantId(req)));   // 数据隔离
        if (status != null && !status.isBlank() && !"全部".equals(status)) qw.eq("status", status);
        if (dateFrom != null && !dateFrom.isBlank()) qw.ge("create_time", dateFrom.trim());
        if (dateTo != null && !dateTo.isBlank()) qw.le("create_time", dateTo.trim() + " 23:59:59");
        qw.orderByDesc("id");

        StringBuilder sb = new StringBuilder("\uFEFF订单号,房源,房型,入住人,手机号,入住日期,离店日期,晚数,金额,状态,下单时间\n");
        for (BookingOrder o : orderMapper.selectList(qw)) {
            House h = houseMapper.selectById(o.getHouseId());
            Room r = roomMapper.selectById(o.getRoomId());
            sb.append(o.getOrderNo()).append(',')
              .append(h != null ? h.getName() : "").append(',')
              .append(r != null ? r.getName() : "").append(',')
              .append(o.getGuestName()).append(',')
              .append(o.getPhone()).append(',')
              .append(o.getCheckIn()).append(',')
              .append(o.getCheckOut()).append(',')
              .append(o.getNights()).append(',')
              .append(o.getAmount()).append(',')
              .append(o.getStatus()).append(',')
              .append(o.getCreateTime()).append('\n');
        }
        String fileName = "orders_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm")) + ".csv";
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bytes);
    }

    private OrderVO toVO(BookingOrder o) {
        OrderVO vo = OrderVO.from(o);
        House h = houseMapper.selectById(o.getHouseId());
        if (h != null) {
            vo.setHouseName(h.getName());
            vo.setImgUrl(HouseVO.imgUrl(h.getImgPrompt()));
        }
        Room r = roomMapper.selectById(o.getRoomId());
        if (r != null) vo.setRoomName(r.getName());
        return vo;
    }
}
