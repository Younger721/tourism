package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.dto.AuthResponse;
import com.travel.dto.LoginRequest;
import com.travel.dto.RegisterRequest;
import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserMapper userMapper;
    private final TokenService tokenService;

    public AuthController(UserMapper userMapper, TokenService tokenService) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    /**
     * 注册普通用户，并立即创建登录令牌。
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        log.info("用户注册请求：用户名={}", request.getUsername());
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            log.warn("注册失败：用户名已存在 [{}]", request.getUsername());
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        String token = tokenService.createToken(user);
        log.info("用户注册成功 [ID={}, 用户名={}]", user.getId(), user.getUsername());
        return ApiResponse.ok(new AuthResponse(token, user));
    }

    /**
     * 校验账号密码，并创建新的登录令牌。
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求：用户名={}", request.getUsername());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPassword, request.getPassword()));
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            log.warn("登录失败：用户名或密码错误 [用户名={}]", request.getUsername());
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenService.createToken(user);
        log.info("用户登录成功 [ID={}, 用户名={}, 角色={}]", user.getId(), user.getUsername(), user.getRole());
        return ApiResponse.ok(new AuthResponse(token, user));
    }

    /**
     * 退出当前 Sa-Token 登录会话。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        log.info("用户退出登录请求");
        tokenService.logoutCurrent();
        return ApiResponse.ok(null);
    }
}