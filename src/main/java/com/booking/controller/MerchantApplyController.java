package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.mapper.AdminMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 商家入驻申请（公开，无需登录）
 */
@RestController
@RequestMapping("/api/merchant")
public class MerchantApplyController {

    private final AdminMapper adminMapper;

    public MerchantApplyController(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    /**
     * 提交入驻申请
     */
    @PostMapping("/apply")
    @CrossOrigin
    public Result<Map<String, Object>> apply(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String merchantName = body.get("merchantName");
        String contactName = body.get("contactName");
        String contactPhone = body.get("contactPhone");
        String description = body.get("description");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error(400, "请设置登录账号和密码");
        }
        if (merchantName == null || merchantName.isBlank()) {
            return Result.error(400, "请填写民宿/酒店名称");
        }
        if (contactPhone == null || !contactPhone.matches("^1\\d{10}$")) {
            return Result.error(400, "请填写正确的联系人手机号");
        }
        Admin exist = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", username.trim()));
        if (exist != null) {
            // 仅"被平台拒绝"的申请可重新提交（复用原账号并覆盖资料）；其余一律占用
            boolean rejected = "拒绝".equals(exist.getApplyStatus());
            if (!rejected || exist.getStatus() != 0) {
                return Result.error(400, "该登录账号已被占用");
            }
            exist.setPassword(password);
            exist.setNickname(contactName == null || contactName.isBlank() ? merchantName.trim() : contactName.trim());
            exist.setMerchantId(0);
            exist.setMerchantName(merchantName.trim());
            exist.setContactPhone(contactPhone.trim());
            exist.setDescription(description == null || description.isBlank() ? null : description.trim());
            exist.setApplyStatus("申请中");
            exist.setRejectReason(null);
            adminMapper.updateById(exist);
            Map<String, Object> existData = new HashMap<>();
            existData.put("id", exist.getId());
            existData.put("merchantName", exist.getMerchantName());
            return Result.ok(existData);
        }

        Admin a = new Admin();
        a.setUsername(username.trim());
        a.setPassword(password);
        a.setNickname(contactName == null || contactName.isBlank() ? merchantName.trim() : contactName.trim());
        a.setRole("merchant");
        a.setMerchantId(0);          // 审核通过时分配
        a.setMerchantName(merchantName.trim());
        a.setContactPhone(contactPhone.trim());
        a.setDescription(description == null || description.isBlank() ? null : description.trim());
        a.setOpenStatus(1);        // 默认营业
        a.setApplyStatus("申请中");
        a.setStatus(0);              // 未启用，审核通过后启用
        a.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        adminMapper.insert(a);

        Map<String, Object> data = new HashMap<>();
        data.put("id", a.getId());
        data.put("merchantName", a.getMerchantName());
        return Result.ok(data);
    }
}
