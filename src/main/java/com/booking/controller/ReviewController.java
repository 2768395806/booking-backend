package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.booking.dto.Result;
import com.booking.entity.House;
import com.booking.entity.Review;
import com.booking.mapper.HouseMapper;
import com.booking.mapper.ReviewMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户端-房源评论
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ReviewController {

    private final ReviewMapper reviewMapper;
    private final HouseMapper houseMapper;

    public ReviewController(ReviewMapper reviewMapper, HouseMapper houseMapper) {
        this.reviewMapper = reviewMapper;
        this.houseMapper = houseMapper;
    }

    /**
     * 房源评论列表（仅显示已上架评论，含平均评分）
     */
    @GetMapping("/houses/{houseId}/reviews")
    public Result<Map<String, Object>> list(@PathVariable Integer houseId,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        QueryWrapper<Review> qw = new QueryWrapper<>();
        qw.eq("house_id", houseId).eq("status", 1).orderByDesc("id");
        Page<Review> p = reviewMapper.selectPage(new Page<>(page, size), qw);

        // 平均评分（已上架评论）
        Double avg = reviewMapper.selectList(
                new QueryWrapper<Review>().eq("house_id", houseId).eq("status", 1).select("rating"))
                .stream()
                .mapToInt(r -> r.getRating() == null ? 5 : r.getRating())
                .average()
                .orElse(0.0);

        Map<String, Object> data = new HashMap<>();
        data.put("total", p.getTotal());
        data.put("avgRating", Math.round(avg * 10) / 10.0);
        data.put("list", p.getRecords());
        return Result.ok(data);
    }

    /**
     * 提交评论
     */
    @PostMapping("/reviews")
    public Result<Review> create(@RequestBody Review review) {
        if (review.getHouseId() == null) return Result.error(400, "请选择房源");
        if (houseMapper.selectById(review.getHouseId()) == null) return Result.error(404, "房源不存在");
        if (review.getContent() == null || review.getContent().isBlank()) {
            return Result.error(400, "请填写评价内容");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            review.setRating(5);
        }
        review.setStatus(1);
        review.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        reviewMapper.insert(review);

        // 房源评价数 +1
        House h = houseMapper.selectById(review.getHouseId());
        if (h != null) {
            h.setReviews((h.getReviews() == null ? 0 : h.getReviews()) + 1);
            houseMapper.updateById(h);
        }
        return Result.ok(review);
    }
}
