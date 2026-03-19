package com.rcs.system.wes;

import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.PickingOrderRepository;
import com.rcs.system.service.CommunicationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class WesSocketService {

    @Autowired
    private PickingOrderRepository orderRepository;

    @Autowired
    private CommunicationLogService communicationLogService;

    @Autowired
    private WesSocketClient wesSocketClient;

    @Autowired
    private WesSocketMessageParser parser;

    @Autowired
    private WesSocketMessageBuilder builder;

    public PickingOrder handleIncomingWesMessage(String rawMessage, String remoteAddress) {
        communicationLogService.saveLogAsync("IN", "TCP", "WES_RECEIVE", rawMessage, null, null, remoteAddress);
        WesSocketMessage message = parser.parse(rawMessage, remoteAddress);

        PickingOrder order = new PickingOrder();
        order.setCommandNo(message.getSceneCode());
        order.setCommandStatus("Pending");
        order.setJobStatus("Received from WES Socket");
        order.setCarrierId(message.getInbound());
        order.setProductId(message.getProductId());
        order.setOrderQty(message.getQuantity());
        order.setJobNo(generateJobNo(message));
        order.setComment(message.getProductName());
        order.setOrderNo(message.getSceneDescription());
        order.setOrderlineNo(message.getRemoteAddress());
        return orderRepository.save(order);
    }

    public void sendResultToWesByJobNo(String jobNo) {
        PickingOrder order = orderRepository.findByJobNo(jobNo)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobNo));
        sendResultToWes(order);
    }

    public void sendResultToWes(PickingOrder order) {
        String message = builder.buildResultMessage(order);
        wesSocketClient.sendToWes(message, order.getJobNo(), order.getCarrierId());
    }

    public void sendRawToWes(String message, String jobNo, String carrierId) {
        wesSocketClient.sendToWes(message, jobNo, carrierId);
    }

    public String getWesRemoteAddress() {
        return wesSocketClient.getRemoteAddress();
    }

    private String generateJobNo(WesSocketMessage message) {
        String prefix = Optional.ofNullable(message.getSceneCode()).filter(v -> !v.isBlank()).orElse("WES");
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
