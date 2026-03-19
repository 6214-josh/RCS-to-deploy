package com.rcs.system.tcp;

import com.rcs.system.service.CommunicationLogService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.AttributeKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Component
public class AccupickTcpServer {

    private final int port = 10000; // Default TCP port for AccuPick
    public static final AttributeKey<AccupickTcpServer> SERVER_INSTANCE = AttributeKey.newInstance("AccupickTcpServerInstance");
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Autowired
    private CommunicationLogService communicationLogService;

    // Store active TCP client channels
    private final ConcurrentMap<String, Channel> activeTcpChannels = new ConcurrentHashMap<>();

    // Callback for sending messages to WebSocket clients
    private Consumer<String> websocketMessageSender;

    public void setWebsocketMessageSender(Consumer<String> sender) {
        this.websocketMessageSender = sender;
    }

    public void start() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.attr(AccupickTcpServer.SERVER_INSTANCE).set(AccupickTcpServer.this);
                     ch.pipeline().addLast(
                             new ByteArrayDecoder(),
                             new ByteArrayEncoder(),
                             new AccupickTcpServerHandler(communicationLogService, AccupickTcpServer.this::sendToWebsocket)
                     );
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            serverChannel = b.bind(port).sync().channel();
            System.out.println("AccuPick TCP Server started on port " + port);
        } catch (Exception e) {
            System.err.println("Failed to start AccuPick TCP Server: " + e.getMessage());
            throw e;
        }
    }

    @PreDestroy
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        System.out.println("AccuPick TCP Server stopped.");
    }

    // Method to send data to a specific AccuPick TCP client
    public void sendToAccupick(String remoteAddress, byte[] data) {
        Channel channel = activeTcpChannels.get(remoteAddress);
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(data);
            communicationLogService.saveLogAsync("OUT", "TCP", "REQUEST", new String(data), null, null, remoteAddress);
        } else {
            System.err.println("TCP client " + remoteAddress + " not found or not active.");
            communicationLogService.saveLogAsync("OUT", "TCP", "ERROR", "Client not found or not active: " + remoteAddress, null, null, remoteAddress);
        }
    }

    // Internal method to send data to WebSocket clients
    private void sendToWebsocket(String message) {
        if (websocketMessageSender != null) {
            websocketMessageSender.accept(message);
        }
    }

    // Add/Remove active TCP channels
    public void addChannel(String remoteAddress, Channel channel) {
        activeTcpChannels.put(remoteAddress, channel);
        System.out.println("TCP Client connected: " + remoteAddress);
        sendToWebsocket("TCP_CONNECTED:" + remoteAddress);
    }

    public void removeChannel(String remoteAddress) {
        activeTcpChannels.remove(remoteAddress);
        System.out.println("TCP Client disconnected: " + remoteAddress);
        sendToWebsocket("TCP_DISCONNECTED:" + remoteAddress);
    }

    public ConcurrentMap<String, Channel> getActiveTcpChannels() {
        return activeTcpChannels;
    }
}

