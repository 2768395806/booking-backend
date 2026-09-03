package com.booking.controller;

import com.booking.dto.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信小程序登录
 *
 * 流程：小程序 wx.login 拿到 code → 后端用 code + appid + secret 调微信 code2session 换 openid
 * → 记录/返回 openid，作为小程序端用户标识（可后续绑定手机号关联订单）。
 *
 * 注意：code2session 必须配置 wechat.secret（微信公众平台-开发管理-开发设置 中获取），
 * 未配置时接口返回明确提示。
 */
@RestController
@RequestMapping("/api/wx")
@CrossOrigin
public class WxAuthController {

    private final JdbcTemplate jdbc;

    @Value("${wechat.appid:}")
    private String appid;

    @Value("${wechat.secret:}")
    private String secret;

    public WxAuthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 微信登录：code -> openid
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body == null ? null : body.get("code");
        if (code == null || code.isBlank()) {
            return Result.error(400, "缺少微信登录 code");
        }
        if (secret == null || secret.isBlank()) {
            return Result.error(500, "服务端未配置微信 secret，请在 application.properties 配置 wechat.secret");
        }
        String openid = code2session(code);
        if (openid == null) {
            return Result.error(400, "微信登录失败（code 无效或已过期）");
        }
        // 记录用户（存在则复用）
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM wx_user WHERE openid = ?", Integer.class, openid);
        if (cnt == null || cnt == 0) {
            jdbc.update("INSERT INTO wx_user (openid) VALUES (?)", openid);
        }
        // 返回已绑定手机号（如有）
        String phone = null;
        try {
            phone = jdbc.queryForObject("SELECT phone FROM wx_user WHERE openid = ?", String.class, openid);
        } catch (Exception ignored) {
        }
        Map<String, Object> data = new HashMap<>();
        data.put("openid", openid);
        data.put("phone", phone == null ? "" : phone);
        return Result.ok(data);
    }

    /**
     * 绑定/更新手机号（openid + phone）
     */
    @PostMapping("/bind")
    public Result<Void> bind(@RequestBody Map<String, String> body) {
        String openid = body == null ? null : body.get("openid");
        String phone = body == null ? null : body.get("phone");
        if (openid == null || openid.isBlank()) {
            return Result.error(400, "缺少 openid");
        }
        if (phone == null || !phone.matches("^1\\d{10}$")) {
            return Result.error(400, "请填写正确的手机号");
        }
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM wx_user WHERE openid = ?", Integer.class, openid);
        if (cnt == null || cnt == 0) {
            jdbc.update("INSERT INTO wx_user (openid, phone) VALUES (?, ?)", openid, phone.trim());
        } else {
            jdbc.update("UPDATE wx_user SET phone = ? WHERE openid = ?", phone.trim(), openid);
        }
        return Result.ok();
    }

    /** 调微信 code2session 换 openid（失败返回 null） */
    private String code2session(String code) {
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid="
                    + URLEncoder.encode(appid, StandardCharsets.UTF_8)
                    + "&secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8)
                    + "&js_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String json = resp.body();
            if (json != null && json.contains("\"openid\"")) {
                int i = json.indexOf("\"openid\":\"");
                if (i >= 0) {
                    String rest = json.substring(i + "\"openid\":\"".length());
                    int end = rest.indexOf('"');
                    if (end > 0) return rest.substring(0, end);
                }
            }
            // 打印错误便于排查（errcode/errmsg）
            if (json != null) {
                System.out.println("[wx] code2session failed: " + json);
            }
            return null;
        } catch (Exception e) {
            System.out.println("[wx] code2session exception: " + e.getMessage());
            return null;
        }
    }
}
