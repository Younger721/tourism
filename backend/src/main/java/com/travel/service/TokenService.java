package com.travel.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TokenService {
    private final UserMapper userMapper;

    public TokenService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public String createToken(User user) {
        StpUtil.login(user.getId(), SaLoginParameter.create().setIsShare(false));
        user.setPassword(null);
        return StpUtil.getTokenValue();
    }

    public User requireUser(HttpServletRequest request) {
        try {
            return loadActiveUser(StpUtil.getLoginIdAsLong());
        } catch (NotLoginException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
    }

    public void logoutCurrent() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    public void kickoutUser(Long userId) {
        if (userId != null) {
            StpUtil.kickout(userId);
        }
    }

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
            return null;
        }
    }

    public User requireAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无管理员权限");
        }
        return user;
    }

    private User loadActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
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
