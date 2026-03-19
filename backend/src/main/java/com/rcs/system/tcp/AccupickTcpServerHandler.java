package com.rcs.system.tcp;

import com.rcs.system.service.CommunicationLogService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class AccupickTcpServerHandler extends ChannelInboundHandlerAdapter {

    private final CommunicationLogService communicationLogService;
    private final Consumer<String> websocketMessageSender;
    private String remoteAddress;

    public AccupickTcpServerHandler(CommunicationLogService communicationLogService, Consumer<String> websocketMessageSender) {
        this.communicationLogService = communicationLogService;
        this.websocketMessageSender = websocketMessageSender;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress addr = (InetSocketAddress) ctx.channel().remoteAddress();
        remoteAddress = addr.getHostString() + ":" + addr.getPort();
        ctx.channel().attr(AccupickTcpServer.SERVER_INSTANCE).get().addChannel(remoteAddress, ctx.channel());
        communicationLogService.saveLogAsync("IN", "TCP", "CONNECTION", "Client connected", null, null, remoteAddress);
        websocketMessageSender.accept("TCP_LOG: Client " + remoteAddress + " connected.");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().attr(AccupickTcpServer.SERVER_INSTANCE).get().removeChannel(remoteAddress);
        communicationLogService.saveLogAsync("OUT", "TCP", "DISCONNECTION", "Client disconnected", null, null, remoteAddress);
        websocketMessageSender.accept("TCP_LOG: Client " + remoteAddress + " disconnected.");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        byte[] data = (byte[]) msg;
        String receivedMessage = new String(data);
        communicationLogService.saveLogAsync("IN", "TCP", "RESPONSE", receivedMessage, null, null, remoteAddress);
        websocketMessageSender.accept("TCP_DATA:" + remoteAddress + ":" + receivedMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("TCP Handler error for " + remoteAddress + ": " + cause.getMessage());
        communicationLogService.saveLogAsync("ERROR", "TCP", "EXCEPTION", cause.getMessage(), null, null, remoteAddress);
        websocketMessageSender.accept("TCP_LOG: Error for " + remoteAddress + ": " + cause.getMessage());
        ctx.close();
    }
}
