package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.ApiResponse;
import com.travel.entity.FriendRequest;
import com.travel.entity.Friendship;
import com.travel.entity.User;
import com.travel.mapper.FriendRequestMapper;
import com.travel.mapper.FriendshipMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.FriendService;
import com.travel.service.TokenService;
import com.travel.websocket.ChatWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendRequestMapper friendRequestMapper;
    private final FriendshipMapper friendshipMapper;
    private final UserMapper userMapper;
    private final TokenService tokenService;
    private final FriendService friendService;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public FriendController(FriendRequestMapper friendRequestMapper, FriendshipMapper friendshipMapper,
                            UserMapper userMapper, TokenService tokenService, FriendService friendService,
                            ChatWebSocketHandler chatWebSocketHandler) {
        this.friendRequestMapper = friendRequestMapper;
        this.friendshipMapper = friendshipMapper;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.friendService = friendService;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @GetMapping
    public ApiResponse<List<User>> friends(HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        List<Long> ids = friendshipMapper.selectList(new LambdaQueryWrapper<Friendship>()
                        .eq(Friendship::getUserId, user.getId()))
                .stream().map(Friendship::getFriendId).toList();
        if (ids.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        List<User> users = userMapper.selectBatchIds(ids);
        users.forEach(item -> item.setPassword(null));
        return ApiResponse.ok(users);
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendRequest>> requests(HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        return ApiResponse.ok(friendRequestMapper.selectList(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getToUserId, user.getId())
                .eq(FriendRequest::getStatus, "PENDING")
                .orderByDesc(FriendRequest::getCreateTime)));
    }

    @PostMapping("/requests")
    public ApiResponse<FriendRequest> requestFriend(@RequestBody FriendRequest friendRequest, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        if (friendRequest.getToUserId() == null || friendRequest.getToUserId().equals(user.getId())) {
            throw new IllegalArgumentException("请选择正确的用户");
        }
        if (friendService.areFriends(user.getId(), friendRequest.getToUserId())) {
            throw new IllegalArgumentException("你们已经是好友");
        }
        Long count = friendRequestMapper.selectCount(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUserId, user.getId())
                .eq(FriendRequest::getToUserId, friendRequest.getToUserId())
                .eq(FriendRequest::getStatus, "PENDING"));
        if (count > 0) {
            throw new IllegalArgumentException("好友申请已发送");
        }
        friendRequest.setFromUserId(user.getId());
        friendRequest.setStatus("PENDING");
        friendRequestMapper.insert(friendRequest);
        chatWebSocketHandler.sendFriendEvent(friendRequest.getToUserId(), "REQUESTED", user.getId());
        return ApiResponse.ok(friendRequest);
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResponse<Void> accept(@PathVariable Long id, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        FriendRequest friendRequest = friendRequestMapper.selectById(id);
        if (friendRequest == null || !friendRequest.getToUserId().equals(user.getId())) {
            throw new IllegalArgumentException("好友申请不存在");
        }
        friendRequest.setStatus("ACCEPTED");
        friendRequestMapper.updateById(friendRequest);
        friendService.createFriendship(friendRequest.getFromUserId(), friendRequest.getToUserId());
        chatWebSocketHandler.sendFriendEvent(friendRequest.getFromUserId(), "ACCEPTED", friendRequest.getToUserId());
        chatWebSocketHandler.sendFriendEvent(friendRequest.getToUserId(), "ACCEPTED", friendRequest.getFromUserId());
        return ApiResponse.ok(null);
    }

    @PostMapping("/requests/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        FriendRequest friendRequest = friendRequestMapper.selectById(id);
        if (friendRequest == null || !friendRequest.getToUserId().equals(user.getId())) {
            throw new IllegalArgumentException("好友申请不存在");
        }
        friendRequest.setStatus("REJECTED");
        friendRequestMapper.updateById(friendRequest);
        chatWebSocketHandler.sendFriendEvent(friendRequest.getFromUserId(), "REJECTED", friendRequest.getToUserId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> delete(@PathVariable Long friendId, HttpServletRequest request) {
        User user = tokenService.requireUser(request);
        friendshipMapper.delete(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, user.getId())
                .eq(Friendship::getFriendId, friendId));
        friendshipMapper.delete(new LambdaQueryWrapper<Friendship>()
                .eq(Friendship::getUserId, friendId)
                .eq(Friendship::getFriendId, user.getId()));
        return ApiResponse.ok(null);
    }
}
