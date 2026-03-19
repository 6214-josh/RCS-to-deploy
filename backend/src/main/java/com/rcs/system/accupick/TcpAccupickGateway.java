package com.rcs.system.accupick;

import com.rcs.system.accupick.event.AccupickAckReceivedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class TcpAccupickGateway implements AccupickGateway {

    private final ApplicationEventPublisher eventPublisher;

    @Value("${rcs.accupick.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${rcs.accupick.host:127.0.0.1}")
    private String host;

    @Value("${rcs.accupick.port:10000}")
    private int port;

    @Value("${rcs.accupick.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    private final Object connectionLock = new Object();
    private volatile Socket socket;
    private volatile BufferedWriter writer;
    private volatile BufferedReader reader;
    private volatile Thread listenerThread;

    public TcpAccupickGateway(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public AccupickAck sendCommand(String payload, int timeoutMs) {
        if (mockEnabled) {
            throw new IllegalStateException("TCP gateway disabled in mock mode");
        }

        try {
            ensureConnected(timeoutMs);
            synchronized (connectionLock) {
                writer.write(payload);
                writer.write("\n");
                writer.flush();
            }
            return null;
        } catch (Exception e) {
            resetConnection();
            throw new RuntimeException("AccuPick TCP communication failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String mode() {
        return mockEnabled ? "MOCK" : String.format("TCP %s:%d (persistent)", host, port);
    }

    private void ensureConnected(int timeoutMs) throws IOException {
        if (isConnected()) {
            startListenerIfNeeded();
            return;
        }

        synchronized (connectionLock) {
            if (isConnected()) {
                startListenerIfNeeded();
                return;
            }

            resetConnection();
            Socket newSocket = new Socket();
            newSocket.connect(new InetSocketAddress(host, port), Math.max(timeoutMs, connectTimeoutMs));
            newSocket.setKeepAlive(true);
            newSocket.setTcpNoDelay(true);
            socket = newSocket;
            writer = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8));
            log.info("Connected to AccuPick {}:{}", host, port);
            startListenerIfNeeded();
        }
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && writer != null && reader != null;
    }

    private void startListenerIfNeeded() {
        if (listenerThread != null && listenerThread.isAlive()) {
            return;
        }
        listenerThread = new Thread(this::listenLoop, "accupick-ack-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        try {
            while (isConnected()) {
                String line = reader.readLine();
                if (line == null) {
                    log.warn("AccuPick connection closed by remote peer");
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }
                AccupickAck ack = parseAck(line);
                log.info("Received ACK from AccuPick: {}", line);
                eventPublisher.publishEvent(new AccupickAckReceivedEvent(ack));
            }
        } catch (Exception e) {
            log.warn("AccuPick listener stopped: {}", e.getMessage());
        } finally {
            resetConnection();
        }
    }

    public AccupickAck parseAck(String raw) {
        Map<String, String> values = KvCsvUtil.decode(raw);
        return AccupickAck.builder()
                .commandNo(firstNonBlank(values.get("Command_No"), values.get("command_no")))
                .commandStatus(firstNonBlank(values.get("Command_Status"), values.get("command_status"), "NG"))
                .actualQty(parseInteger(firstNonBlank(values.get("Actual_qty"), values.get("actual_qty"))))
                .abnormalReasonCode(parseInteger(firstNonBlank(values.get("Abnormal_reason_code"), values.get("abnormal_reason_code"))))
                .commandTime(parseLong(firstNonBlank(values.get("Command_time"), values.get("command_time"))))
                .errorDetail(firstNonBlank(values.get("Error_detail"), values.get("error_detail")))
                .rawMessage(raw)
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private void resetConnection() {
        synchronized (connectionLock) {
            closeQuietly(reader);
            closeQuietly(writer);
            closeQuietly(socket);
            reader = null;
            writer = null;
            socket = null;
            listenerThread = null;
        }
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    @PreDestroy
    public void destroy() {
        resetConnection();
    }
}
