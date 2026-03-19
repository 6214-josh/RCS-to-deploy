package com.rcs.system.service;

import com.rcs.system.accupick.AccupickGateway;
import com.rcs.system.dto.DashboardSummary;
import com.rcs.system.model.CommunicationLog;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.CommunicationLogRepository;
import com.rcs.system.repository.PickingOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final PickingOrderRepository pickingOrderRepository;
    private final CommunicationLogRepository communicationLogRepository;
    private final AccupickGateway accupickGateway;

    @Value("${server.port:8088}")
    private String serverPort;

    public DashboardSummary getSummary(int websocketClients) {
        long totalOrders = pickingOrderRepository.count();
        long inQueue = pickingOrderRepository.countByCommandStatus("Pending");
        long processing = pickingOrderRepository.countByCommandStatus("Processing");
        long success = pickingOrderRepository.countByCommandStatus("OK");
        long ngErrors = pickingOrderRepository.countByCommandStatus("NG") + pickingOrderRepository.countByCommandStatus("COMM_TIMEOUT");

        return DashboardSummary.builder()
                .totalOrders(totalOrders)
                .inQueue(inQueue)
                .processing(processing)
                .success(success)
                .ngErrors(ngErrors)
                .accupickMode(accupickGateway.mode())
                .dashboardWebsocket("ws://<RCS_HOST>:" + serverPort + "/ws/dashboard | clients=" + websocketClients)
                .build();
    }

    public List<PickingOrder> getRecentOrders(int limit) {
        return pickingOrderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.max(limit, 1)));
    }

    public List<PickingOrder> getQueueOrders() {
        return pickingOrderRepository.findByCommandStatusInOrderByCreatedAtDesc(Arrays.asList("Pending", "Processing"));
    }

    public List<CommunicationLog> getRecentLogs(int limit) {
        return communicationLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, Math.max(limit, 1)));
    }
}
