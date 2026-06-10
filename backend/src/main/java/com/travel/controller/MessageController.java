package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.auth.CurrentUser;
import com.travel.auth.RequireLogin;
import com.travel.common.ApiResponse;
import com.travel.dto.ConversationResponse;
import com.travel.entity.ChatMessage;
import com.travel.entity.User;
import com.travel.mapper.ChatMessageMapper;
import com.travel.mapper.UserMapper;
import com.travel.service.FriendService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequireLogin
@RequestMapping("/api/messages")
public class MessageController {
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;
    private final FriendService friendService;

    public MessageController(ChatMessageMapper chatMessageMapper, UserMapper userMapper, FriendService friendService) {
        this.chatMessageMapper = chatMessageMapper;
        this.userMapper = userMapper;
        this.friendService = friendService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> conversations(@CurrentUser User user) {
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, user.getId()).or().eq(ChatMessage::getReceiverId, user.getId()))
                .orderByDesc(ChatMessage::getCreateTime));
        Map<Long, ChatMessage> latest = new LinkedHashMap<>();
        for (ChatMessage message : messages) {
            Long friendId = message.getSenderId().equals(user.getId()) ? message.getReceiverId() : message.getSenderId();
            latest.putIfAbsent(friendId, message);
        }
        List<ConversationResponse> result = new ArrayList<>();
        for (Map.Entry<Long, ChatMessage> entry : latest.entrySet()) {
            User friend = userMapper.selectById(entry.getKey());
            if (friend == null) {
                continue;
            }
            friend.setPassword(null);
            Long unread = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getSenderId, entry.getKey())
                    .eq(ChatMessage::getReceiverId, user.getId())
                    .eq(ChatMessage::getReadStatus, 0));
            result.add(new ConversationResponse(friend, entry.getValue(), unread));
        }
        return ApiResponse.ok(result);
    }

    @GetMapping
    public ApiResponse<List<ChatMessage>> history(@RequestParam Long friendId, @CurrentUser User user) {
        if (!friendService.areFriends(user.getId(), friendId)) {
            throw new IllegalArgumentException("只有好友之间可以查看私信");
        }
        List<ChatMessage> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(w -> w.eq(ChatMessage::getSenderId, user.getId()).eq(ChatMessage::getReceiverId, friendId)
                        .or()
                        .eq(ChatMessage::getSenderId, friendId).eq(ChatMessage::getReceiverId, user.getId()))
                .orderByAsc(ChatMessage::getCreateTime));
        List<ChatMessage> unread = messages.stream()
                .filter(item -> item.getReceiverId().equals(user.getId()) && Objects.equals(item.getReadStatus(), 0))
                .collect(Collectors.toList());
        unread.forEach(item -> {
            item.setReadStatus(1);
            chatMessageMapper.updateById(item);
        });
        return ApiResponse.ok(messages);
    }
}
