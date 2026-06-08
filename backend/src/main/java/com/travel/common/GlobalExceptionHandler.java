package com.travel.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 将带 HTTP 状态码的异常转换成统一接口响应。
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "请求失败" : ex.getReason();
        log.warn("接口状态异常 状态码={} 消息={}", ex.getStatusCode(), message);
        return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.fail(message));
    }

    /**
     * 处理 Sa-Token 登录失效异常。
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException ex) {
        log.warn("登录状态异常：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("登录已失效，请重新登录"));
    }

    /**
     * 处理角色或权限不足异常。
     */
    @ExceptionHandler({NotRoleException.class, NotPermissionException.class})
    public ResponseEntity<ApiResponse<Void>> handleForbidden(RuntimeException ex) {
        log.warn("权限异常：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("无管理员权限"));
    }

    /**
     * 处理参数校验和业务参数错误。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException ex) {
        log.warn("请求参数错误：{}", ex.getMessage());
        return ApiResponse.fail(ex.getMessage() == null ? "请求参数错误" : ex.getMessage());
    }

    /**
     * 处理未预期的服务端异常，并保持统一响应格式。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handle(Exception ex) {
        log.error("系统未处理异常", ex);
        return ApiResponse.fail(ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }
}
