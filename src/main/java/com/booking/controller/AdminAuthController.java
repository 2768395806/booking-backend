package com.booking.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.config.AdminAuthInterceptor;
import com.booking.config.TokenService;
import com.booking.dto.Result;
import com.booking.entity.Admin;
import com.booking.mapper.AdminMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 商家/平台 认证接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final AdminMapper adminMapper;
    private final TokenService tokenService;

    public AdminAuthController(AdminMapper adminMapper, TokenService tokenService) {
        this.adminMapper = adminMapper;
        this.tokenService = tokenService;
    }

    /**
     * 登录（商家与平台共用）
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.error(400, "请输入用户名和密码");
        }
        Admin admin = adminMapper.selectOne(
                new QueryWrapper<Admin>().eq("username", username.trim()));
        if (admin == null || !admin.getPassword().equals(password)) {
            return Result.error(400, "用户名或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            if ("申请中".equals(admin.getApplyStatus())) {
                return Result.error(403, "入驻申请审核中，请耐心等待平台审核");
            }
            if ("拒绝".equals(admin.getApplyStatus())) {
                return Result.error(403, "入驻申请未通过：" + (admin.getRejectReason() == null ? "" : admin.getRejectReason()));
            }
            return Result.error(403, "账号已停用，请联系平台");
        }
        // 平台开发者：仅预留手机号（contactPhone）持有人可登录
        if ("platform".equals(admin.getRole())) {
            String expect = admin.getContactPhone();
            if (expect == null || expect.isBlank()) {
                return Result.error(403, "开发者端尚未配置预留手机号，请先在平台初始化");
            }
            String phone = body.get("phone") == null ? "" : body.get("phone").trim();
            if (!expect.equals(phone)) {
                return Result.error(403, "手机号不符：开发者端仅限预留手机号登录");
            }
        }
        String token = tokenService.create(admin.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("role", admin.getRole());
        data.put("merchantId", admin.getMerchantId());
        data.put("merchantName", admin.getMerchantName());
        return Result.ok(data);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.remove(auth.substring(7));
        }
        return Result.ok();
    }

    /**
     * 当前登录信息
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile(HttpServletRequest request) {
        Admin admin = (Admin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        Map<String, Object> data = new HashMap<>();
        data.put("username", admin.getUsername());
        data.put("nickname", admin.getNickname());
        data.put("role", admin.getRole());
        data.put("merchantId", admin.getMerchantId());
        data.put("merchantName", admin.getMerchantName());
        data.put("contactPhone", admin.getContactPhone());
        data.put("description", admin.getDescription());
        data.put("openStatus", admin.getOpenStatus());
        data.put("address", admin.getAddress());
        data.put("lng", admin.getLng());
        data.put("lat", admin.getLat());
        return Result.ok(data);
    }

    /**
     * 更新商家资料（名称不可修改，联系人在入驻审核后由平台处理）
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Admin admin = (Admin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        if (body.containsKey("nickname")) {
            String v = body.get("nickname");
            admin.setNickname(v == null || v.isBlank() ? null : v.trim());
        }
        if (body.containsKey("contactPhone")) {
            String v = body.get("contactPhone");
            admin.setContactPhone(v == null || v.isBlank() ? null : v.trim());
        }
        if (body.containsKey("description")) {
            String v = body.get("description");
            admin.setDescription(v == null || v.isBlank() ? null : v.trim());
        }
        if (body.containsKey("address")) {
            String v = body.get("address");
            admin.setAddress(v == null || v.isBlank() ? null : v.trim());
        }
        if (body.containsKey("lng")) {
            admin.setLng(parseDouble(body.get("lng")));
        }
        if (body.containsKey("lat")) {
            admin.setLat(parseDouble(body.get("lat")));
        }
        adminMapper.updateById(admin);
        return Result.ok();
    }

    /** 宽松解析坐标字符串，非法/空返回 null */
    private static Double parseDouble(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 修改登录密码
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
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
        // 密码变更后强制重新登录
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.remove(auth.substring(7));
        }
        return Result.ok();
    }

    /**
     * 营业状态开关：1=营业中，0=暂停接单
     */
    @PutMapping("/open-status")
    public Result<Void> updateOpenStatus(HttpServletRequest request, @RequestBody Map<String, Integer> body) {
        Admin admin = (Admin) request.getAttribute(AdminAuthInterceptor.ATTR_ADMIN);
        Integer open = body.get("openStatus");
        if (open == null || (open != 0 && open != 1)) {
            return Result.error(400, "营业状态参数错误");
        }
        admin.setOpenStatus(open);
        adminMapper.updateById(admin);
        return Result.ok();
    }
}
