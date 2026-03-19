package com.rcs.system.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcs.system.model.CommunicationLog;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.service.DashboardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardWebSocketService {

    private final DashboardWebSocketHandler handler;
    private final DashboardQueryService dashboardQueryService;
    private final ObjectMapper objectMapper;

    @jakarta.annotation.PostConstruct
    public void init() {
        handler.setDashboardWebSocketService(this);
    }

    public void sendInit(WebSocketSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "dashboard:init");
        payload.put("summary", dashboardQueryService.getSummary(handler.connectedCount()));
        payload.put("orders", dashboardQueryService.getRecentOrders(50));
        payload.put("logs", dashboardQueryService.getRecentLogs(100));
        send(session, payload);
    }

    public void publishSummary() {
        broadcast(Map.of(
                "type", "dashboard:summary",
                "summary", dashboardQueryService.getSummary(handler.connectedCount())
        ));
    }

    public void publishOrder(PickingOrder order) {
        broadcast(Map.of(
                "type", "dashboard:order-updated",
                "order", order,
                "summary", dashboardQueryService.getSummary(handler.connectedCount())
        ));
    }

    public void publishLog(CommunicationLog log) {
        broadcast(Map.of(
                "type", "dashboard:log",
                "log", log,
                "summary", dashboardQueryService.getSummary(handler.connectedCount())
        ));
    }

    private void broadcast(Map<String, Object> payload) {
        try {
            handler.broadcast(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ignored) {
        }
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
        }
    }
}
