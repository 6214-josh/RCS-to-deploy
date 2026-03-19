package com.rcs.system.config;

import com.rcs.system.dashboard.DashboardWebSocketHandler;
import com.rcs.system.websocket.AccupickWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AccupickWebSocketHandler accupickWebSocketHandler;
    private final DashboardWebSocketHandler dashboardWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(accupickWebSocketHandler, "/ws/accupick").setAllowedOriginPatterns("*");
        registry.addHandler(dashboardWebSocketHandler, "/ws/dashboard").setAllowedOriginPatterns("*");
    }
}
