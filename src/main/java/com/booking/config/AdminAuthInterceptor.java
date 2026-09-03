package com.booking.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.booking.entity.Admin;
import com.booking.mapper.AdminMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权：校验 Bearer token，并按角色限制访问路径
 * /api/admin/**   → 商家（merchant）
 * /api/platform/** → 平台开发者（platform）
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_ADMIN = "currentAdmin";
    public static final String ROLE_MERCHANT = "merchant";
    public static final String ROLE_PLATFORM = "platform";

    private final TokenService tokenService;
    private final AdminMapper adminMapper;

    public AdminAuthInterceptor(TokenService tokenService, AdminMapper adminMapper) {
        this.tokenService = tokenService;
        this.adminMapper = adminMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        String username = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            username = tokenService.verify(auth.substring(7));
        }
        if (username == null) {
            return reject(response, 401, "未登录或登录已过期");
        }

        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", username));
        if (admin == null) {
            return reject(response, 401, "账号不存在");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            return reject(response, 403, "账号已停用，请联系平台");
        }

        // 路径-角色匹配
        String uri = request.getRequestURI();
        String needRole = null;
        if (uri.startsWith("/api/platform/")) needRole = ROLE_PLATFORM;
        else if (uri.startsWith("/api/admin/")) {
            needRole = ROLE_MERCHANT;
            // 通用账号接口（资料/密码/退出/营业状态/上传）对商家与平台开发者都放行
            if (isCommonAccountUri(uri)) needRole = null;
        }

        if (needRole != null && !needRole.equals(admin.getRole())) {
            return reject(response, 403, "无权访问");
        }

        request.setAttribute(ATTR_ADMIN, admin);
        return true;
    }

    /** 平台开发者同样需要使用的通用账号接口前缀 */
    private boolean isCommonAccountUri(String uri) {
        return uri.startsWith("/api/admin/profile")
                || uri.startsWith("/api/admin/password")
                || uri.startsWith("/api/admin/logout")
                || uri.startsWith("/api/admin/open-status")
                || uri.startsWith("/api/admin/upload");
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(code);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
        return false;
    }
}
