package com.travel.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.entity.User;
import com.travel.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AuthWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthWebSocketHandler.class);

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public AuthWebSocketHandler(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 注册已认证的浏览器标签页用于强制登出通知。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = resolveUser(session);
        if (user == null) {
            log.warn("认证WebSocket连接被拒绝 sessionId={}", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("userId", user.getId());
        Set<WebSocketSession> sessions = userSessions.computeIfAbsent(user.getId(), key -> new CopyOnWriteArraySet<>());
        sessions.add(session);
        log.info("认证WebSocket连接成功 userId={} sessionId={} 活跃标签数={}",
                user.getId(), session.getId(), sessions.size());
    }

    /**
     * 从内存认证套接字注册表中移除已关闭的浏览器标签页。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
        }
        log.info("认证WebSocket连接关闭 userId={} sessionId={} 状态={} 剩余标签数={}",
                userId, session.getId(), status.getCode(), sessions.size());
    }

    /**
     * 向用户的所有活跃标签页发送强制登出事件。
     */
    public void notifyKickout(Long userId, String reason) {
        Set<WebSocketSession> sessions = userSessions.remove(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("没有可通知的认证WebSocket会话 userId={}", userId);
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "KICKOUT");
        payload.put("message", reason);
        int notified = 0;
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
                session.close(CloseStatus.POLICY_VIOLATION.withReason(reason));
                notified++;
            } catch (Exception ex) {
                log.warn("认证WebSocket强制登出通知失败 userId={} sessionId={} 错误={}",
                        userId, session.getId(), ex.getMessage());
            }
        }
        log.info("认证WebSocket强制登出通知完成 userId={} 会话数={} 已通知数={}",
                userId, sessions.size(), notified);
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