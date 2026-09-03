package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booking.config.AdminAuthInterceptor;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.entity.House;
import com.booking.entity.Review;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.ReviewMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家后台-评论管理
 */
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final ReviewMapper reviewMapper;
    private final HouseMapper houseMapper;

    public AdminReviewController(ReviewMapper reviewMapper, HouseMapper houseMapper) {
        this.reviewMapper = reviewMapper;
        this.houseMapper = houseMapper;
    }

    private Integer merchantId(HttpServletRequest req) {
        Admin a = (Admin) req.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        return a == null ? 0 : (a.getMerchantId() == null ? 0 : a.getMerchantId());
    }

    /** 当前商家名下的评论，无归属返回 null */
    private Review ownedReview(HttpServletRequest req, Integer id) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("id", id).inSql("house_id",
                "SELECT id FROM house WHERE merchant_id = " + merchantId(req));
        return reviewMapper.selectOne(qw);
    }

    /**
     * 评论列表（分页 + 筛选）
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            HttpServletRequest req,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer houseId,
            @RequestParam(required = false) Integer status) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.inSql("house_id", "SELECT id FROM house WHERE merchant_id = " + merchantId(req));   // 数据隔离
        if (houseId != null) qw.eq("house_id", houseId);
        if (status != null) qw.eq("status", status);
        qw.orderByDesc("id");

        Page<Review> p = reviewMapper.selectPage(new Page<>(page, size), qw);
        List<Map<String, Object>> list = p.getRecords().stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("houseId", r.getHouseId());
            House h = houseMapper.selectById(r.getHouseId());
            m.put("houseName", h != null ? h.getName() : "");
            m.put("guestName", r.getGuestName());
            m.put("phone", r.getPhone());
            m.put("rating", r.getRating());
            m.put("content", r.getContent());
            m.put("reply", r.getReply());
            m.put("status", r.getStatus());
            m.put("createTime", r.getCreateTime());
            m.put("replyTime", r.getReplyTime());
            return m;
        }).toList();

        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("page", p.getCurrent());
        data.put("size", p.getSize());
        data.put("list", list);
        return Result.ok(data);
    }

    /**
     * 商家回复
     */
    @PostMapping("/{id}/reply")
    public Result<Void> reply(HttpServletRequest req, @PathVariable Integer id, @RequestBody Map<String, String> body) {
        Review review = ownedReview(req, id);
        if (review == null) return Result.error(404, "评论不存在");
        String reply = body != null ? body.get("reply") : null;
        if (reply == null || reply.isBlank()) return Result.error(400, "请输入回复内容");
        review.setReply(reply.trim());
        review.setReplyTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        reviewMapper.updateById(review);
        return Result.ok();
    }

    /**
     * 上架/隐藏评论
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(HttpServletRequest req, @PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Review review = ownedReview(req, id);
        if (review == null) return Result.error(404, "评论不存在");
        Integer status = body != null ? body.get("status") : null;
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "status 必须为 0 或 1");
        }
        review.setStatus(status);
        reviewMapper.updateById(review);
        return Result.ok();
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest req, @PathVariable Integer id) {
        if (ownedReview(req, id) == null) return Result.error(404, "评论不存在");
        reviewMapper.deleteById(id);
        return Result.ok();
    }
}
