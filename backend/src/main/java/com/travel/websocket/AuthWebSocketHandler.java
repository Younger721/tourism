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
            log.warn("用户未登录，认证连接被拒绝 [会话ID={}]", session.getId());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("userId", user.getId());
        Set<WebSocketSession> sessions = userSessions.computeIfAbsent(user.getId(), key -> new CopyOnWriteArraySet<>());
        sessions.add(session);
        log.info("用户[{}]已注册认证通道，当前活跃标签数{} [会话ID={}]",
                user.getId(), sessions.size(), session.getId());
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
        log.info("用户[{}]认证通道已关闭，剩余活跃标签数{} [会话ID={}, 状态码={}]",
                userId, sessions.size(), session.getId(), status.getCode());
    }

    /**
     * 向用户的所有活跃标签页发送强制登出事件。
     */
    public void notifyKickout(Long userId, String reason) {
        Set<WebSocketSession> sessions = userSessions.remove(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.info("用户[{}]没有活跃的认证通道，无需通知", userId);
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
                log.warn("通知用户[{}]强制登出时出错 [会话ID={}]：{}",
                        userId, session.getId(), ex.getMessage());
            }
        }
        log.info("已通知用户[{}]强制登出，共{}个会话，成功{}个",
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