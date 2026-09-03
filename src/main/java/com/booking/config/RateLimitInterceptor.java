package com.booking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单防刷限流：对登录与公开写接口按 IP 做每分钟次数限制（内存滑动窗口）。
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** 登录类接口：每分钟最多 8 次（防暴力破解） */
    private static final int LIMIT_LOGIN = 8;
    /** 下单/申请/评论/收藏等：每分钟最多 30 次 */
    private static final int LIMIT_WRITE = 30;
    private static final long WINDOW_MS = 60_000L;

    private static final ConcurrentHashMap<String, Window> store = new ConcurrentHashMap<>();

    private static final class Window {
        long start;
        int count;
        Window(long start, int count) { this.start = start; this.count = count; }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 仅对 POST 写操作限流；GET 读取不受影响
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        boolean login = uri.startsWith("/api/admin/login") || uri.startsWith("/api/wx/login");
        boolean write = uri.startsWith("/api/orders")
                || uri.startsWith("/api/merchant/apply")
                || uri.startsWith("/api/reviews")
                || uri.startsWith("/api/wx/bind")
                || uri.startsWith("/api/favorites/toggle");
        if (!login && !write) return true;

        int limit = login ? LIMIT_LOGIN : LIMIT_WRITE;
        String key = clientIp(request) + "|" + uri;
        long now = System.currentTimeMillis();

        Window w = store.compute(key, (k, v) -> {
            if (v == null || now - v.start >= WINDOW_MS) return new Window(now, 1);
            v.count++;
            return v;
        });

        if (w.count > limit) {
            response.setStatus(429);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"msg\":\"操作过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            int comma = fwd.indexOf(',');
            return (comma > 0 ? fwd.substring(0, comma) : fwd).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
