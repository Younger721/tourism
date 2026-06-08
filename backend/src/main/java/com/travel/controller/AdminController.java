package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.*;
import com.travel.mapper.*;
import com.travel.service.TokenService;
import com.travel.websocket.AuthWebSocketHandler;
import com.travel.websocket.ChatWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ScenicSpotMapper scenicSpotMapper;
    private final UserMapper userMapper;
    private final CommentMapper commentMapper;
    private final FavoriteMapper favoriteMapper;
    private final FootprintMapper footprintMapper;
    private final TravelPostMapper travelPostMapper;
    private final TokenService tokenService;
    private final AuthWebSocketHandler authWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public AdminController(ScenicSpotMapper scenicSpotMapper, UserMapper userMapper, CommentMapper commentMapper,
                           FavoriteMapper favoriteMapper, FootprintMapper footprintMapper, TravelPostMapper travelPostMapper,
                           TokenService tokenService, AuthWebSocketHandler authWebSocketHandler,
                           ChatWebSocketHandler chatWebSocketHandler) {
        this.scenicSpotMapper = scenicSpotMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.favoriteMapper = favoriteMapper;
        this.footprintMapper = footprintMapper;
        this.travelPostMapper = travelPostMapper;
        this.tokenService = tokenService;
        this.authWebSocketHandler = authWebSocketHandler;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("users", userMapper.selectCount(null));
        data.put("spots", scenicSpotMapper.selectCount(null));
        data.put("posts", travelPostMapper.selectCount(null));
        data.put("footprints", footprintMapper.selectCount(null));
        return ApiResponse.ok(data);
    }

    @GetMapping("/scenic-spots")
    public ApiResponse<List<ScenicSpot>> scenicSpots(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        return ApiResponse.ok(scenicSpotMapper.selectList(new LambdaQueryWrapper<ScenicSpot>()
                .orderByDesc(ScenicSpot::getCreateTime)));
    }

    @PostMapping("/scenic-spots")
    public ApiResponse<ScenicSpot> saveScenicSpot(@RequestBody ScenicSpot item, HttpServletRequest request) {
        tokenService.requireAdmin(request);
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getId() == null) {
            scenicSpotMapper.insert(item);
        } else {
            scenicSpotMapper.updateById(item);
        }
        return ApiResponse.ok(item);
    }

    @DeleteMapping("/scenic-spots/{id}")
    public ApiResponse<Void> deleteScenicSpot(@PathVariable Long id, HttpServletRequest request) {
        tokenService.requireAdmin(request);
        scenicSpotMapper.deleteById(id);
        deleteEngagements("SCENIC", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        users.forEach(user -> user.setPassword(null));
        return ApiResponse.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> disableUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        setUserStatus(id, 0, admin);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<User> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusRequest body,
                                              HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        User user = setUserStatus(id, body.status(), admin);
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    @PostMapping("/users/{id}/kickout")
    public ApiResponse<Void> kickoutUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        assertNotSelf(admin, id);
        assertUserExists(id);
        kickoutAndCloseSocket(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/posts")
    public ApiResponse<List<TravelPost>> posts(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        return ApiResponse.ok(travelPostMapper.selectList(new LambdaQueryWrapper<TravelPost>().orderByDesc(TravelPost::getCreateTime)));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        tokenService.requireAdmin(request);
        travelPostMapper.deleteById(id);
        deleteEngagements("POST", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/footprints")
    public ApiResponse<List<Footprint>> footprints(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        return ApiResponse.ok(footprintMapper.selectList(new LambdaQueryWrapper<Footprint>().orderByDesc(Footprint::getCreateTime)));
    }

    @DeleteMapping("/footprints/{id}")
    public ApiResponse<Void> deleteFootprint(@PathVariable Long id, HttpServletRequest request) {
        tokenService.requireAdmin(request);
        footprintMapper.deleteById(id);
        return ApiResponse.ok(null);
    }

    private User setUserStatus(Long userId, Integer status, User admin) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("用户状态只能是 0 或 1");
        }
        assertNotSelf(admin, userId);
        User user = assertUserExists(userId);
        user.setStatus(status);
        userMapper.updateById(user);
        if (status == 0) {
            kickoutAndCloseSocket(userId);
        }
        return user;
    }

    private User assertUserExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return user;
    }

    private void assertNotSelf(User admin, Long targetUserId) {
        if (admin.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("不能对当前管理员账号执行此操作");
        }
    }

    private void kickoutAndCloseSocket(Long userId) {
        tokenService.kickoutUser(userId);
        authWebSocketHandler.notifyKickout(userId, "管理员已强制下线");
        chatWebSocketHandler.closeUserSession(userId, "管理员已强制下线");
    }

    private void deleteEngagements(String targetType, Long targetId) {
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, targetType)
                .eq(Comment::getTargetId, targetId));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getTargetType, targetType)
                .eq(Favorite::getTargetId, targetId));
    }

    public record UserStatusRequest(Integer status) {
    }
}
