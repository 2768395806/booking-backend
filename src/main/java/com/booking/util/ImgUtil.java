package com.booking.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 图片地址工具：把数据库中的相对地址（/uploads/...）转为小程序可访问的绝对地址。
 * 已带 http/https 的地址（含 AI 生成图）原样返回。
 */
public final class ImgUtil {

    private ImgUtil() {
    }

    /**
     * 根据当前请求的 Host 补全上传图片地址。
     * @param url 原始地址：http(s) 绝对地址 / /uploads/ 相对地址 / 空
     */
    public static String absolute(HttpServletRequest req, String url) {
        if (url == null || url.isBlank()) return "";
        String v = url.trim();
        if (v.startsWith("/uploads/") && req != null) {
            String host = req.getHeader("Host");
            if (host != null && !host.isBlank()) {
                return req.getScheme() + "://" + host + v;
            }
        }
        return v;
    }

    /**
     * 完整转换：兼容库中既存真实上传地址（/uploads、http）也兼容 AI prompt 占位文本。
     * @param urlOrPrompt 图片地址或 AI 图描述 prompt
     */
    public static String full(HttpServletRequest req, String urlOrPrompt) {
        if (urlOrPrompt == null || urlOrPrompt.isBlank()) return "";
        String v = urlOrPrompt.trim();
        if (v.startsWith("/uploads/")) return absolute(req, v);
        if (v.startsWith("http")) return v;
        String encoded = URLEncoder.encode(v, StandardCharsets.UTF_8);
        return "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt="
                + encoded + "&image_size=landscape_4_3";
    }
}
