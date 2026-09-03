package com.booking.config;

import com.booking.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常兜底：避免未捕获异常导致 500 HTML 或内部堆栈泄露，统一返回 JSON。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public Result<Void> handle(Exception e, HttpServletRequest req) {
        log.error("未捕获异常: {} {}", req.getMethod(), req.getRequestURI(), e);
        return Result.error(500, "服务器繁忙，请稍后重试");
    }
}
