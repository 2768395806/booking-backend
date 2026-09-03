package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 房源接口（C 端）
 */
@RestController
@RequestMapping("/api/houses")
@CrossOrigin
public class HouseController {

    private final HouseMapper houseMapper;
    private final RoomMapper roomMapper;
    private final AdminMapper adminMapper;
    private final BookingOrderMapper orderMapper;

    public HouseController(HouseMapper houseMapper, RoomMapper roomMapper, AdminMapper adminMapper, BookingOrderMapper orderMapper) {
        this.houseMapper = houseMapper;
        this.roomMapper = roomMapper;
        this.adminMapper = adminMapper;
        this.orderMapper = orderMapper;
    }

    /**
     * 房源列表（支持搜索 / 类型 / 区域 / 商家 / 排序）
     */
    @GetMapping
    public List<HouseVO> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Integer merchantId,
            @RequestParam(required = false) String sort,
            HttpServletRequest req) {

        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("status", 1);   // 用户端仅展示上架房源
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            qw.and(w -> w.like("name", k).or().like("area", k).or().like("address", k).or().like("tags", k));
        }
        if (type != null && !type.isBlank() && !"全部".equals(type)) {
            qw.eq("type", type);
        }
        if (area != null && !area.isBlank() && !"全部".equals(area)) {
            qw.eq("area", area);
        }
        if (merchantId != null) {
            qw.eq("merchant_id", merchantId);   // 按商家筛选
        }
        if ("priceAsc".equals(sort)) {
            qw.orderByAsc("price");
        } else if ("priceDesc".equals(sort)) {
            qw.orderByDesc("price");
        } else if ("score".equals(sort)) {
            qw.orderByDesc("score");
        } else {
            qw.orderByDesc("id");
        }

        List<House> houses = houseMapper.selectList(qw);
        // 批量查商家信息（名称 + 营业状态）
        Map<Integer, Admin> adminMap = merchantMap(houses);
        List<HouseVO> result = new ArrayList<>();
        for (House h : houses) {
            HouseVO vo = HouseVO.from(h);
            vo.setImgUrl(ImgUtil.absolute(req, vo.getImgUrl()));   // 真实上传图补全为绝对地址
            Admin m = adminMap.get(h.getMerchantId());
            if (m != null) {
                vo.setMerchantName(m.getMerchantName());
                vo.setOpenStatus(m.getOpenStatus());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 有在售房源的商家列表（C 端商家筛选用）
     */
    @GetMapping("/merchants")
    public List<Map<String, Object>> merchants() {
        // 找出有上架房源的商家
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("status", 1).select("DISTINCT merchant_id");
        List<House> houses = houseMapper.selectList(qw);
        Set<Integer> ids = houses.stream()
                .map(House::getMerchantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer id : ids) {
            Admin m = adminMapper.selectById(id);
            if (m == null) continue;
            result.add(Map.of("id", id, "merchantName", m.getMerchantName(),
                    "openStatus", m.getOpenStatus() == null ? 1 : m.getOpenStatus()));
        }
        return result;
    }

    /**
     * 房源详情（含房型列表）
     */
    @GetMapping("/{id}")
    public HouseVO detail(@PathVariable Integer id, HttpServletRequest req) {
        House h = houseMapper.selectById(id);
        if (h == null || (h.getStatus() != null && h.getStatus() == 0)) return null;
        HouseVO vo = HouseVO.from(h);
        vo.setImgUrl(ImgUtil.absolute(req, vo.getImgUrl()));
        Admin m = adminMapper.selectById(h.getMerchantId());
        if (m != null) {
            vo.setMerchantName(m.getMerchantName());
            vo.setMerchantPhone(m.getContactPhone());
            vo.setMerchantAddress(m.getAddress());
            vo.setMerchantLng(m.getLng());
            vo.setMerchantLat(m.getLat());
            vo.setOpenStatus(m.getOpenStatus());
        }
        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.eq("house_id", id).orderByAsc("price");
        List<Room> rooms = roomMapper.selectList(qw);
        for (Room r : rooms) {
            r.setImgUrl(ImgUtil.full(req, r.getImg()));   // 房型图（上传地址或 AI prompt → 可访问 URL）
        }
        vo.setRooms(rooms);
        return vo;
    }

    /**
     * 房型按日期区间可用性（按订单重叠动态计算，非全局库存）
     * checkIn/checkOut 格式 yyyy-MM-dd；缺省默认 明天~后天
     */
    @GetMapping("/{id}/availability")
    public List<Map<String, Object>> availability(
            @PathVariable Integer id,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            HttpServletRequest req) {

        List<Map<String, Object>> result = new ArrayList<>();
        if (houseMapper.selectById(id) == null) return result;

        String in = (checkIn == null || checkIn.isBlank())
                ? java.time.LocalDate.now().plusDays(1).toString() : checkIn.trim();
        String out = (checkOut == null || checkOut.isBlank())
                ? java.time.LocalDate.now().plusDays(2).toString() : checkOut.trim();
        if (out.compareTo(in) <= 0) return result;

        QueryWrapper<Room> qw = new QueryWrapper<>();
        qw.eq("house_id", id).orderByAsc("price");
        for (Room r : roomMapper.selectList(qw)) {
            QueryWrapper<BookingOrder> oqw = new QueryWrapper<>();
            oqw.eq("room_id", r.getId())
               .ne("status", "已取消")
               .ne("status", "已完成")   // 已离店/已取消不占房
               .lt("check_in", out)      // ISO 日期字符串可直接字典序比较
               .gt("check_out", in);
            int occupied = Math.toIntExact(orderMapper.selectCount(oqw));
            int total = r.getStock() == null ? 1 : r.getStock();
            int available = Math.max(0, total - occupied);
            result.add(Map.of(
                    "roomId", r.getId(),
                    "name", r.getName(),
                    "price", r.getPrice(),
                    "imgUrl", ImgUtil.full(req, r.getImg()),
                    "total", total,
                    "occupied", occupied,
                    "available", available,
                    "soldOut", available <= 0));
        }
        return result;
    }

    private Map<Integer, Admin> merchantMap(List<House> houses) {
        Set<Integer> ids = houses.stream()
                .map(House::getMerchantId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return adminMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Admin::getId, Function.identity()));
    }
}
