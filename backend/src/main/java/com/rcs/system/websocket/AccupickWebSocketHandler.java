package com.rcs.system.websocket;

import com.rcs.system.service.CommunicationLogService;
import com.rcs.system.tcp.AccupickTcpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AccupickWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Autowired
    private CommunicationLogService communicationLogService;

    @Autowired
    private AccupickTcpServer accupickTcpServer;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        communicationLogService.saveLogAsync("IN", "WEBSOCKET", "CONNECTION", "WebSocket client connected", null, null, session.getRemoteAddress().toString());
        System.out.println("WebSocket session established: " + session.getId());
        // Set the sender for TCP server to send messages to WebSocket clients
        accupickTcpServer.setWebsocketMessageSender(this::sendToAllSessions);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        communicationLogService.saveLogAsync("IN", "WEBSOCKET", "REQUEST", payload, null, null, session.getRemoteAddress().toString());
        System.out.println("Received WebSocket message: " + payload);

        // Example: Frontend sends a command to AccuPick via TCP
        // Format: TCP_SEND:<remoteAddress>:<data>
        if (payload.startsWith("TCP_SEND:")) {
            String[] parts = payload.split(":", 3);
            if (parts.length == 3) {
                String remoteAddress = parts[1];
                String dataToSend = parts[2];
                accupickTcpServer.sendToAccupick(remoteAddress, dataToSend.getBytes());
                communicationLogService.saveLogAsync("OUT", "TCP", "REQUEST", dataToSend, null, null, remoteAddress);
            } else {
                sendToSession(session, "ERROR: Invalid TCP_SEND format.");
            }
        } else if (payload.startsWith("TCP_GET_ACTIVE_CLIENTS")) {
            // Send active TCP client list to the requesting session
            StringBuilder sb = new StringBuilder("ACTIVE_TCP_CLIENTS:");
            accupickTcpServer.getActiveTcpChannels().keySet().forEach(addr -> sb.append(addr).append(","));
            sendToSession(session, sb.toString());
        }
        // Add other WebSocket command handling here
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        communicationLogService.saveLogAsync("OUT", "WEBSOCKET", "DISCONNECTION", "WebSocket client disconnected", null, null, session.getRemoteAddress().toString());
        System.out.println("WebSocket session closed: " + session.getId() + " with status " + status.getCode());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        communicationLogService.saveLogAsync("ERROR", "WEBSOCKET", "EXCEPTION", exception.getMessage(), null, null, session.getRemoteAddress().toString());
        System.err.println("WebSocket transport error: " + exception.getMessage());
    }

    public void sendToAllSessions(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    communicationLogService.saveLogAsync("OUT", "WEBSOCKET", "RESPONSE", message, null, null, session.getRemoteAddress().toString());
                } catch (IOException e) {
                    System.err.println("Error sending WebSocket message to session " + session.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public void sendToSession(WebSocketSession session, String message) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                communicationLogService.saveLogAsync("OUT", "WEBSOCKET", "RESPONSE", message, null, null, session.getRemoteAddress().toString());
            } catch (IOException e) {
                System.err.println("Error sending WebSocket message to session " + session.getId() + ": " + e.getMessage());
            }
        }
    }
}
