package com.rcs.system.wes;

import com.rcs.system.service.CommunicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class WesSocketClient {

    @Value("${rcs.wes.client.host:127.0.0.1}")
    private String wesHost;

    @Value("${rcs.wes.client.port:10001}")
    private int wesPort;

    @Value("${rcs.wes.client.timeout-ms:5000}")
    private int timeoutMs;

    @Autowired
    private CommunicationLogService communicationLogService;

    public void sendToWes(String message, String jobNo, String carrierId) {
        String remote = wesHost + ":" + wesPort;
        try (Socket socket = new Socket(wesHost, wesPort)) {
            socket.setSoTimeout(timeoutMs);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            writer.println(message);
            communicationLogService.saveLogAsync("OUT", "TCP", "WES_SEND", message, jobNo, carrierId, remote);
        } catch (Exception e) {
            communicationLogService.saveLogAsync("ERROR", "TCP", "WES_SEND_ERROR", e.getMessage() + " | message=" + message, jobNo, carrierId, remote);
            throw new RuntimeException("Send to WES failed: " + e.getMessage(), e);
        }
    }

    public String getRemoteAddress() {
        return wesHost + ":" + wesPort;
    }
}
