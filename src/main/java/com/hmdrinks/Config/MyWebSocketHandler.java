package com.hmdrinks.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.hmdrinks.Service.JwtService;
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {
    private final Map<Integer, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    public MyWebSocketHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> queryParams = extractQueryParams(session.getUri());
        Integer userId = queryParams.containsKey("userId") ? parseInt(queryParams.get("userId")) : null;
        String token = queryParams.get("token");

        if (userId != null && isValidToken(userId, token)) {
            sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        } else {
            System.out.println("❌ Kết nối bị từ chối: Token không hợp lệ");
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.forEach((userId, sessionSet) -> {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                sessions.remove(userId);
            }
        });
    }

    public void sendMessageToUser(Integer userId, String message, Integer shipmentId, String type) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions != null) {
            for (WebSocketSession session : userSessions) {
                if (session.isOpen()) {
                    try {
                        // Định dạng JSON phù hợp với frontend
                        String jsonMessage = objectMapper.writeValueAsString(Map.of(
                                "type", type,
                                "userId", userId,
                                "shipmentId", shipmentId,
                                "message", message,
                                "time", System.currentTimeMillis() // Thời gian gửi
                        ));

                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        System.err.println("❌ Lỗi gửi tin nhắn đến User ID " + userId + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    public void sendMessageToUser2(Integer senderId, Integer receiverId, String message, Integer shipmentId, String type) {
        Set<WebSocketSession> userSessions = sessions.get(receiverId);
        if (userSessions != null) {
            for (WebSocketSession session : userSessions) {
                if (session.isOpen()) {
                    try {
                        // Định dạng JSON phù hợp với frontend
                        String jsonMessage = objectMapper.writeValueAsString(Map.of(
                                "type", type,
                                "senderId", senderId,
                                "receiverId", receiverId,
                                "shipmentId", shipmentId,
                                "message", message,
                                "time", System.currentTimeMillis() // Thời gian gửi
                        ));

                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        System.err.println("❌ Lỗi gửi tin nhắn đến User ID " + receiverId + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private Map<String, String> extractQueryParams(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(uri.getQuery().split("&"))
                .map(param -> param.split("=", 2))
                .filter(pair -> pair.length == 2)
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isValidToken(Integer userId, String token) {
        try {
            String extractedUserId = jwtService.extractUserId(token);
            return extractedUserId != null && extractedUserId.equals(userId.toString());
        } catch (Exception e) {
            return false;
        }
    }
}

