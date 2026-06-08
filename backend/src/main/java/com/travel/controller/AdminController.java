package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.Comment;
import com.travel.entity.Favorite;
import com.travel.entity.Footprint;
import com.travel.entity.ScenicSpot;
import com.travel.entity.TravelPost;
import com.travel.entity.User;
import com.travel.mapper.CommentMapper;
import com.travel.mapper.FavoriteMapper;
import com.travel.mapper.FootprintMapper;
import com.travel.mapper.ScenicSpotMapper;
import com.travel.mapper.TravelPostMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.TokenService;
import com.travel.websocket.AuthWebSocketHandler;
import com.travel.websocket.ChatWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

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

    /**
     * 返回管理员首页的仪表盘统计数据。
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats(HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        Map<String, Long> data = new LinkedHashMap<>();
        data.put("users", userMapper.selectCount(null));
        data.put("spots", scenicSpotMapper.selectCount(null));
        data.put("posts", travelPostMapper.selectCount(null));
        data.put("footprints", footprintMapper.selectCount(null));
        log.info("管理员统计数据已加载 adminId={} 数据={}", admin.getId(), data);
        return ApiResponse.ok(data);
    }

    @GetMapping("/scenic-spots")
    public ApiResponse<List<ScenicSpot>> scenicSpots(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        return ApiResponse.ok(scenicSpotMapper.selectList(new LambdaQueryWrapper<ScenicSpot>()
                .orderByDesc(ScenicSpot::getCreateTime)));
    }

    /**
     * 从管理控制台创建或更新景点记录。
     */
    @PostMapping("/scenic-spots")
    public ApiResponse<ScenicSpot> saveScenicSpot(@RequestBody ScenicSpot item, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getId() == null) {
            scenicSpotMapper.insert(item);
            log.info("管理员创建景点 adminId={} 景点ID={} 名称={}", admin.getId(), item.getId(), item.getName());
        } else {
            scenicSpotMapper.updateById(item);
            log.info("管理员更新景点 adminId={} 景点ID={} 名称={}", admin.getId(), item.getId(), item.getName());
        }
        return ApiResponse.ok(item);
    }

    /**
     * 删除景点并清理相关的评论和收藏。
     */
    @DeleteMapping("/scenic-spots/{id}")
    public ApiResponse<Void> deleteScenicSpot(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        scenicSpotMapper.deleteById(id);
        deleteEngagements("SCENIC", id);
        log.info("管理员删除景点 adminId={} 景点ID={}", admin.getId(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        users.forEach(user -> user.setPassword(null));
        return ApiResponse.ok(users);
    }

    /**
     * 禁用用户账户并强制用户下线。
     */
    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> disableUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        log.info("管理员禁用用户 adminId={} 目标用户ID={}", admin.getId(), id);
        setUserStatus(id, 0, admin);
        return ApiResponse.ok(null);
    }

    /**
     * 启用或禁用用户；禁用时同时使活跃会话失效。
     */
    @PatchMapping("/users/{id}/status")
    public ApiResponse<User> updateUserStatus(@PathVariable Long id, @RequestBody UserStatusRequest body,
                                              HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        log.info("管理员更新用户状态 adminId={} 目标用户ID={} 状态={}", admin.getId(), id, body.status());
        User user = setUserStatus(id, body.status(), admin);
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    /**
     * 强制用户下线而不改变账户状态。
     */
    @PostMapping("/users/{id}/kickout")
    public ApiResponse<Void> kickoutUser(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        log.info("管理员强制用户下线 adminId={} 目标用户ID={}", admin.getId(), id);
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

    /**
     * 删除社区帖子及其评论和收藏。
     */
    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        travelPostMapper.deleteById(id);
        deleteEngagements("POST", id);
        log.info("管理员删除帖子 adminId={} 帖子ID={}", admin.getId(), id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/footprints")
    public ApiResponse<List<Footprint>> footprints(HttpServletRequest request) {
        tokenService.requireAdmin(request);
        return ApiResponse.ok(footprintMapper.selectList(new LambdaQueryWrapper<Footprint>().orderByDesc(Footprint::getCreateTime)));
    }

    /**
     * 删除用户足迹记录。
     */
    @DeleteMapping("/footprints/{id}")
    public ApiResponse<Void> deleteFootprint(@PathVariable Long id, HttpServletRequest request) {
        User admin = tokenService.requireAdmin(request);
        footprintMapper.deleteById(id);
        log.info("管理员删除足迹 adminId={} 足迹ID={}", admin.getId(), id);
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
        log.info("用户状态已更新 adminId={} 目标用户ID={} 状态={}", admin.getId(), userId, status);
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
        log.info("用户套接字已关闭（强制下线后） userId={}", userId);
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