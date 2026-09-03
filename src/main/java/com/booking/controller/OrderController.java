package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.dto.OrderCreateReq;
import com.booking.dto.OrderVO;
import com.booking.dto.HouseVO;
import com.booking.entity.Admin;
import com.booking.entity.BookingOrder;
import com.booking.entity.House;
import com.booking.entity.Room;
import com.booking.mapper.AdminMapper;
import com.booking.mapper.BookingOrderMapper;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.RoomMapper;
import com.booking.util.ImgUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 预约订单接口
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final BookingOrderMapper orderMapper;
    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final AdminMapper adminMapper;

    public OrderController(BookingOrderMapper orderMapper, HouseMapper houseMapper, RoomMapper roomMapper, AdminMapper adminMapper) {
        this.orderMapper = orderMapper;
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
        this.adminMapper = adminMapper;
    }

    /**
     * 创建预约订单
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody OrderCreateReq req) {
        if (req.getHouseId() == null || req.getRoomId() == null) {
            return Map.of("code", 400, "msg", "请选择房源和房型");
        }
        if (req.getGuestName() == null || req.getGuestName().isBlank()) {
            return Map.of("code", 400, "msg", "请填写入住人姓名");
        }
        if (req.getPhone() == null || !req.getPhone().matches("^1\\d{10}$")) {
            return Map.of("code", 400, "msg", "请填写正确的手机号");
        }

        Room room = roomMapper.selectById(req.getRoomId());
        if (room == null) {
            return Map.of("code", 400, "msg", "房型不存在");
        }
        if (room.getStock() != null && room.getStock() <= 0) {
            return Map.of("code", 400, "msg", "该房型已订满");
        }
        House house = houseMapper.selectById(req.getHouseId());
        if (house == null) {
            return Map.of("code", 400, "msg", "房源不存在");
        }
        // 商家暂停接单时禁止下单
        if (house.getMerchantId() != null && house.getMerchantId() > 0) {
            Admin m = adminMapper.selectById(house.getMerchantId());
            if (m != null && m.getOpenStatus() != null && m.getOpenStatus() == 0) {
                return Map.of("code", 400, "msg", "商家暂停接单中，暂无法预约");
            }
        }

        // ---- 日期处理：缺省 明天入住 / 后天离店，须 yyyy-MM-dd 且离店晚于入住 ----
        String in = (req.getCheckIn() == null || req.getCheckIn().isBlank())
                ? java.time.LocalDate.now().plusDays(1).toString() : req.getCheckIn().trim();
        String out = (req.getCheckOut() == null || req.getCheckOut().isBlank())
                ? java.time.LocalDate.now().plusDays(2).toString() : req.getCheckOut().trim();
        if (!in.matches("\\d{4}-\\d{2}-\\d{2}") || !out.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return Map.of("code", 400, "msg", "日期格式不正确");
        }
        if (out.compareTo(in) <= 0) {
            return Map.of("code", 400, "msg", "离店日期需晚于入住日期");
        }

        // ---- 按日期区间动态校验是否满房（已取消订单不占房） ----
        QueryWrapper<BookingOrder> oqw = new QueryWrapper<>();
        oqw.eq("room_id", room.getId())
           .ne("status", "已取消")
           .ne("status", "已完成")   // 已离店/已取消订单不占房
           .lt("check_in", out)
           .gt("check_out", in);
        long occupied = orderMapper.selectCount(oqw);
        int total = room.getStock() == null ? 1 : room.getStock();
        if (occupied >= total) {
            return Map.of("code", 400, "msg", "该房型在所选日期已满房，请调整日期或选择其他房型");
        }

        int nights = (req.getNights() == null || req.getNights() < 1)
                ? Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.LocalDate.parse(in), java.time.LocalDate.parse(out)))
                : req.getNights();
        BigDecimal amount = room.getPrice().multiply(BigDecimal.valueOf(nights));

        BookingOrder order = new BookingOrder();
        order.setOrderNo(generateOrderNo());
        order.setHouseId(req.getHouseId());
        order.setRoomId(req.getRoomId());
        order.setGuestName(req.getGuestName().trim());
        order.setPhone(req.getPhone().trim());
        order.setCheckIn(in);
        order.setCheckOut(out);
        order.setNights(nights);
        order.setAmount(amount);
        order.setStatus("待确认");
        order.setRemark(req.getRemark() == null ? "" : req.getRemark().trim());
        order.setOpenid(req.getOpenid() == null ? "" : req.getOpenid().trim());
        order.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        orderMapper.insert(order);

        return Map.of("code", 200, "msg", "预约提交成功", "orderNo", order.getOrderNo());
    }

    /**
     * 订单列表（openid 优先，其次按手机号关联）
     */
    @GetMapping
    public List<OrderVO> list(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String openid,
            @RequestParam(required = false) String status,
            HttpServletRequest req) {

        QueryWrapper<BookingOrder> qw = new QueryWrapper<>();
        if (openid != null && !openid.isBlank()) {
            qw.eq("openid", openid.trim());
        } else if (phone != null && !phone.isBlank()) {
            qw.eq("phone", phone.trim());
        }
        if (status != null && !status.isBlank() && !"全部".equals(status)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");

        List<BookingOrder> orders = orderMapper.selectList(qw);
        List<OrderVO> result = new ArrayList<>();
        for (BookingOrder o : orders) {
            OrderVO vo = OrderVO.from(o);
            House h = houseMapper.selectById(o.getHouseId());
            if (h != null) {
                vo.setHouseName(h.getName());
                vo.setImgUrl(ImgUtil.absolute(req, HouseVO.imgUrl(h.getImgPrompt())));
                Admin m = adminMapper.selectById(h.getMerchantId());
                if (m != null) {
                    vo.setMerchantName(m.getMerchantName());
                    vo.setMerchantPhone(m.getContactPhone());
                }
            }
            Room r = roomMapper.selectById(o.getRoomId());
            if (r != null) vo.setRoomName(r.getName());
            result.add(vo);
        }
        return result;
    }

    /**
     * 订单详情（按 id 查询，含商家联系方式）
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Integer id, HttpServletRequest req) {
        BookingOrder o = orderMapper.selectById(id);
        if (o == null) {
            return Map.of("code", 404, "msg", "订单不存在");
        }
        OrderVO vo = OrderVO.from(o);
        House h = houseMapper.selectById(o.getHouseId());
        if (h != null) {
            vo.setHouseName(h.getName());
            vo.setImgUrl(ImgUtil.absolute(req, HouseVO.imgUrl(h.getImgPrompt())));
            Admin m = adminMapper.selectById(h.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getMerchantName());
                vo.setMerchantPhone(m.getContactPhone());
            }
        }
        Room r = roomMapper.selectById(o.getRoomId());
        if (r != null) vo.setRoomName(r.getName());
        return Map.of("code", 200, "msg", "ok", "data", vo);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable Integer id) {
        BookingOrder order = orderMapper.selectById(id);
        if (order == null) {
            return Map.of("code", 404, "msg", "订单不存在");
        }
        if ("已取消".equals(order.getStatus())) {
            return Map.of("code", 200, "msg", "订单已取消");
        }
        order.setStatus("已取消");
        orderMapper.updateById(order);
        return Map.of("code", 200, "msg", "订单已取消");
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(10000, 99999);
        return "B" + date + rand;
    }
}
