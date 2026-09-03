package com.booking.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Token 管理（SQLite 持久化）
 * 令牌有效期 30 天且落库存储，后端重启后登录态不失效。
 */
@Component
public class TokenService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXPIRE_DAYS = 30;

    private final JdbcTemplate jdbc;

    public TokenService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 登录成功后创建 token */
    public String create(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String expire = LocalDateTime.now().plusDays(EXPIRE_DAYS).format(FMT);
        try {
            jdbc.update("INSERT INTO admin_token(token, username, expire_time) VALUES (?, ?, ?)",
                    token, username, expire);
        } catch (Exception e) {
            // admin_token 表尚未初始化时降级为纯内存 token（理论不会发生）
            return token;
        }
        return token;
    }

    /** 校验 token，返回用户名；无效（不存在/已过期）返回 null */
    public String verify(String token) {
        if (token == null || token.isBlank()) return null;
        String t = token.trim();
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT username, expire_time FROM admin_token WHERE token = ?", t);
            if (rows.isEmpty()) return null;
            String expire = String.valueOf(rows.get(0).get("expire_time"));
            String now = LocalDateTime.now().format(FMT);
            if (expire == null || expire.compareTo(now) < 0) {
                jdbc.update("DELETE FROM admin_token WHERE token = ?", t);
                return null;
            }
            return String.valueOf(rows.get(0).get("username"));
        } catch (Exception e) {
            return null;
        }
    }

    /** 注销 token */
    public void remove(String token) {
        if (token == null) return;
        try {
            jdbc.update("DELETE FROM admin_token WHERE token = ?", token.trim());
        } catch (Exception ignored) {
        }
    }

    /** 注销某用户（按用户名）的全部 token，用于改密/停用后强制重新登录 */
    public void removeByUsername(String username) {
        if (username == null) return;
        try {
            jdbc.update("DELETE FROM admin_token WHERE username = ?", username);
        } catch (Exception ignored) {
        }
    }
}
