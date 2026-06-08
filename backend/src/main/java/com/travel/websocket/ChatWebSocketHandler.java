package com.travel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.dto.ChatPayload;
import com.travel.entity.ChatMessage;
import com.travel.entity.User;
import com.travel.mapper.ChatMessageMapper;
import com.travel.service.FriendService;
import com.travel.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final TokenService tokenService;
    private final FriendService friendService;
    private final ChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;
    private final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(TokenService tokenService, FriendService friendService,
                                ChatMessageMapper chatMessageMapper, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.friendService = friendService;
        this.chatMessageMapper = chatMessageMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 验证用户身份并注册实时聊天套接字。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = resolveUser(session);
        if (user == null) {
            log.warn("聊天WebSocket连接被拒绝 sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("userId", user.getId());
        onlineUsers.put(user.getId(), session);
        log.info("聊天WebSocket连接成功 userId={} sessionId={} 在线用户数={}",
                user.getId(), session.getId(), onlineUsers.size());
    }

    /**
     * 持久化单条私聊消息，并在接收者在线时转发消息。
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        ChatPayload payload = objectMapper.readValue(message.getPayload(), ChatPayload.class);
        if (senderId == null || payload.getReceiverId() == null || !StringUtils.hasText(payload.getContent())) {
            log.warn("忽略无效聊天消息 senderId={} sessionId={}", senderId, session.getId());
            return;
        }
        if (!friendService.areFriends(senderId, payload.getReceiverId())) {
            log.warn("拒绝非好友聊天请求 senderId={} receiverId={}", senderId, payload.getReceiverId());
            session.sendMessage(new TextMessage("{\"error\":\"只有好友之间可以私聊\"}"));
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSenderId(senderId);
        chatMessage.setReceiverId(payload.getReceiverId());
        chatMessage.setContent(payload.getContent());
        chatMessage.setReadStatus(0);
        chatMessageMapper.insert(chatMessage);

        String json = objectMapper.writeValueAsString(chatMessage);
        session.sendMessage(new TextMessage(json));
        WebSocketSession receiver = onlineUsers.get(payload.getReceiverId());
        boolean delivered = receiver != null && receiver.isOpen();
        if (delivered) {
            receiver.sendMessage(new TextMessage(json));
        }
        log.info("聊天消息已保存 messageId={} senderId={} receiverId={} 是否在线送达={}",
                chatMessage.getId(), senderId, payload.getReceiverId(), delivered);
    }

    /**
     * 套接字关闭后从在线聊天映射中移除用户。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            onlineUsers.remove(userId);
            log.info("聊天WebSocket连接关闭 userId={} sessionId={} 状态={} 在线用户数={}",
                    userId, session.getId(), status.getCode(), onlineUsers.size());
        }
    }

    /**
     * 管理员强制登出后关闭用户的活跃聊天套接字。
     */
    public void closeUserSession(Long userId, String reason) {
        WebSocketSession session = onlineUsers.remove(userId);
        if (session == null || !session.isOpen()) {
            log.info("没有可关闭的聊天WebSocket会话 userId={}", userId);
            return;
        }
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
            log.info("聊天WebSocket被服务端关闭 userId={} sessionId={}", userId, session.getId());
        } catch (Exception ex) {
            log.warn("聊天WebSocket关闭失败 userId={} 错误={}", userId, ex.getMessage());
        }
    }

    /**
     * 向在线用户推送好友列表更新。
     */
    public void sendFriendEvent(Long userId, String action, Long relatedUserId) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session == null || !session.isOpen()) {
            log.info("好友事件跳过，用户离线 userId={} 操作={}", userId, action);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "FRIEND_UPDATED");
            payload.put("action", action);
            payload.put("relatedUserId", relatedUserId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            log.info("好友事件已发送 userId={} 操作={} 相关用户ID={}", userId, action, relatedUserId);
        } catch (Exception ex) {
            log.warn("好友事件发送失败 userId={} 操作={} 错误={}", userId, action, ex.getMessage());
        }
    }

    private User resolveUser(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String token = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("token");
        return tokenService.getUserByToken(token);
    }
}