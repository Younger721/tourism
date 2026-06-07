package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.dto.UserProfileResponse;
import com.travel.dto.UserSearchResponse;
import com.travel.entity.FriendRequest;
import com.travel.entity.TravelPost;
import com.travel.entity.User;
import com.travel.mapper.FriendRequestMapper;
import com.travel.mapper.TravelPostMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.FriendService;
import com.travel.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserMapper userMapper;
    private final FriendRequestMapper friendRequestMapper;
    private final TravelPostMapper travelPostMapper;
    private final TokenService tokenService;
    private final FriendService friendService;

    public UserController(UserMapper userMapper, FriendRequestMapper friendRequestMapper, TravelPostMapper travelPostMapper,
                          TokenService tokenService, FriendService friendService) {
        this.userMapper = userMapper;
        this.friendRequestMapper = friendRequestMapper;
        this.travelPostMapper = travelPostMapper;
        this.tokenService = tokenService;
        this.friendService = friendService;
    }

    @GetMapping("/{id}/profile")
    public ApiResponse<UserProfileResponse> profile(@PathVariable Long id, HttpServletRequest request) {
        User target = userMapper.selectById(id);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        target.setPassword(null);
        User current = tokenService.getUserByToken(request.getHeader("Authorization"));
        String status = current == null ? "GUEST" : current.getId().equals(id) ? "SELF" :
                friendService.areFriends(current.getId(), id) ? "FRIEND" : "NONE";
        return ApiResponse.ok(new UserProfileResponse(target, status));
    }

    @GetMapping("/search")
    public ApiResponse<List<UserSearchResponse>> search(@RequestParam(required = false) String keyword,
                                                        HttpServletRequest request) {
        User current = tokenService.requireUser(request);
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.ok(List.of());
        }
        String trimmedKeyword = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .and(wrapper -> wrapper.like(User::getUsername, trimmedKeyword)
                        .or()
                        .like(User::getNickname, trimmedKeyword))
                .last("LIMIT 20"));
        List<UserSearchResponse> result = users.stream()
                .map(user -> {
                    user.setPassword(null);
                    String status = resolveFriendStatus(current.getId(), user.getId());
                    return new UserSearchResponse(user, status);
                })
                .toList();
        return ApiResponse.ok(result);
    }

    private String resolveFriendStatus(Long currentUserId, Long targetUserId) {
        if (currentUserId.equals(targetUserId)) {
            return "SELF";
        }
        if (friendService.areFriends(currentUserId, targetUserId)) {
            return "FRIEND";
        }
        Long pending = friendRequestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, currentUserId)
                .eq(FriendRequest::getToUserId, targetUserId)
                .eq(FriendRequest::getStatus, "PENDING"));
        return pending > 0 ? "PENDING" : "NONE";
    }

    @GetMapping("/{id}/posts")
    public ApiResponse<java.util.List<TravelPost>> posts(@PathVariable Long id) {
        return ApiResponse.ok(travelPostMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TravelPost>()
                .eq(TravelPost::getUserId, id)
                .eq(TravelPost::getVisibility, "PUBLIC")
                .orderByDesc(TravelPost::getCreateTime)));
    }

    @PutMapping("/me")
    public ApiResponse<User> updateMe(@RequestBody User profile, HttpServletRequest request) {
        User current = tokenService.requireUser(request);
        current.setNickname(profile.getNickname());
        current.setPhone(profile.getPhone());
        current.setAvatarUrl(profile.getAvatarUrl());
        current.setBio(profile.getBio());
        current.setCity(profile.getCity());
        userMapper.updateById(current);
        current.setPassword(null);
        return ApiResponse.ok(current);
    }
}
