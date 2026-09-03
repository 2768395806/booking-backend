package com.booking.controller;

import com.booking.dto.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 平台开发者图片上传（/api/platform/** 由拦截器校验 platform 角色，商家无法访问）
 */
@RestController
@RequestMapping("/api/platform")
public class PlatformUploadController {

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "请选择图片文件");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.error(400, "图片不能超过 10MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error(400, "仅支持 jpg/png/gif/webp/bmp 格式图片");
        }
        // 内容校验：防止伪装扩展名上传非图片文件
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] head = new byte[12];
            int r = in.read(head);
            if (r <= 0 || !isImageContent(head)) {
                return Result.error(400, "文件内容不是有效图片");
            }
        } catch (IOException e) {
            return Result.error(400, "读取文件失败");
        }

        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path dir = Paths.get("uploads", dateDir).toAbsolutePath();
            Files.createDirectories(dir);
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(fileName);
            file.transferTo(target.toFile());

            String url = "/uploads/" + dateDir + "/" + fileName;
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            return Result.ok(data);
        } catch (IOException e) {
            return Result.error(500, "图片保存失败: " + e.getMessage());
        }
    }

    /** 校验文件头是否为常见图片格式（jpg/png/gif/webp/bmp） */
    private static boolean isImageContent(byte[] h) {
        if (h == null || h.length < 4) return false;
        int b0 = h[0] & 0xFF;
        if (b0 == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return true;
        if (b0 == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') return true;
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') return true;
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') return true;
        if (h[0] == 'B' && h[1] == 'M') return true;
        return false;
    }
}
