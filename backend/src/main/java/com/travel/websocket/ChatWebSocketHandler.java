package com.travel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.dto.ChatPayload;
import com.travel.entity.ChatMessage;
import com.travel.entity.User;
import com.travel.mapper.ChatMessageMapper;
import com.travel.service.FriendService;
import com.travel.service.TokenService;
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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = resolveUser(session);
        if (user == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("userId", user.getId());
        onlineUsers.put(user.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        ChatPayload payload = objectMapper.readValue(message.getPayload(), ChatPayload.class);
        if (senderId == null || payload.getReceiverId() == null || !StringUtils.hasText(payload.getContent())) {
            return;
        }
        if (!friendService.areFriends(senderId, payload.getReceiverId())) {
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
        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(json));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    public void closeUserSession(Long userId, String reason) {
        WebSocketSession session = onlineUsers.remove(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
        } catch (Exception ignored) {
        }
    }

    public void sendFriendEvent(Long userId, String action, Long relatedUserId) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "FRIEND_UPDATED");
            payload.put("action", action);
            payload.put("relatedUserId", relatedUserId);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
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
