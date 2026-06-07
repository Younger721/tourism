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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserMapper userMapper;
    private final TokenService tokenService;

    public AuthController(UserMapper userMapper, TokenService tokenService) {
        this.userMapper = userMapper;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
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
        return ApiResponse.ok(new AuthResponse(token, user));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())
                .eq(User::getPassword, request.getPassword()));
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenService.createToken(user);
        return ApiResponse.ok(new AuthResponse(token, user));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        tokenService.logoutCurrent();
        return ApiResponse.ok(null);
    }
}
