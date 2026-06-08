package com.travel.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final UserMapper userMapper;

    public TokenService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 创建不共享的 Sa-Token 会话，并清理返回用户对象里的密码。
     */
    public String createToken(User user) {
        StpUtil.login(user.getId(), SaLoginParameter.create().setIsShare(false));
        user.setPassword(null);
        log.info("用户[{}]登录成功，用户名：{}，角色：{}", user.getId(), user.getUsername(), user.getRole());
        return StpUtil.getTokenValue();
    }

    /**
     * 解析当前请求用户，未登录或登录失效时直接拒绝请求。
     */
    public User requireUser(HttpServletRequest request) {
        try {
            return loadActiveUser(StpUtil.getLoginIdAsLong());
        } catch (NotLoginException ex) {
            log.warn("请求未授权：用户未登录或登录已过期");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
    }

    /**
     * 当前存在登录会话时退出登录。
     */
    public void logoutCurrent() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
            log.info("用户已退出登录");
        }
    }

    /**
     * 强制指定用户的所有活跃令牌下线。
     */
    public void kickoutUser(Long userId) {
        if (userId != null) {
            StpUtil.kickout(userId);
            log.info("用户[{}]已被强制下线", userId);
        }
    }

    /**
     * 根据原始 token 或 Authorization Bearer 值解析用户。
     */
    public User getUserByToken(String token) {
        String rawToken = normalizeToken(token);
        if (rawToken == null) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(rawToken);
            if (loginId == null) {
                return null;
            }
            return loadActiveUser(Long.valueOf(String.valueOf(loginId)));
        } catch (RuntimeException ex) {
            log.warn("根据令牌解析用户失败：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 要求当前请求用户必须是管理员。
     */
    public User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            log.warn("权限不足：用户[{}]尝试访问管理员功能，当前角色：{}", user.getId(), user.getRole());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无管理员权限");
        }
        return user;
    }

    /**
     * 从数据库加载启用状态的用户，并清理密码字段。
     */
    private User loadActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            log.warn("用户不存在或已被禁用 [ID={}]", userId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号已被禁用或登录已失效");
        }
        user.setPassword(null);
        return user;
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = token.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring(7);
        }
        return value;
    }
}