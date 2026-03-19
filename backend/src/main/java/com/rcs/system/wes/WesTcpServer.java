package com.rcs.system.wes;

import com.rcs.system.service.CommunicationLogService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class WesTcpServer {

    @Value("${rcs.wes.server.port:10010}")
    private int port;

    @Autowired
    private WesSocketService wesSocketService;

    @Autowired
    private CommunicationLogService communicationLogService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;

    public void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        executorService.submit(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                this.serverSocket = ss;
                System.out.println("WES TCP Server started on port " + port);
                while (running.get()) {
                    Socket socket = ss.accept();
                    executorService.submit(() -> handleClient(socket));
                }
            } catch (Exception e) {
                if (running.get()) {
                    System.err.println("WES TCP Server start failed: " + e.getMessage());
                }
            }
        });
    }

    private void handleClient(Socket socket) {
        String remoteAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                wesSocketService.handleIncomingWesMessage(line, remoteAddress);
            }
        } catch (Exception e) {
            communicationLogService.saveLogAsync("ERROR", "TCP", "WES_RECEIVE_ERROR", e.getMessage(), null, null, remoteAddress);
        }
    }

    public int getPort() {
        return port;
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {
        }
        executorService.shutdownNow();
    }
}
