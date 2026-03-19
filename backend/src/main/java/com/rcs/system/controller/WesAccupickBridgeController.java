package com.rcs.system.controller;

import com.rcs.system.model.CommunicationLog;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.CommunicationLogRepository;
import com.rcs.system.repository.PickingOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/bridge")
public class WesAccupickBridgeController {

    private final PickingOrderRepository pickingOrderRepository;
    private final CommunicationLogRepository communicationLogRepository;

    @Value("${rcs.accupick.host:127.0.0.1}")
    private String accupickHost;

    @Value("${rcs.accupick.port:12345}")
    private int accupickPort;

    @Value("${rcs.wes.base-url:http://127.0.0.1:8081/api/v1}")
    private String wesBaseUrl;

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "accupickHost", accupickHost,
                "accupickPort", accupickPort,
                "wesBaseUrl", wesBaseUrl,
                "recentOrderCount", pickingOrderRepository.count(),
                "processingCount", pickingOrderRepository.countByCommandStatus("Processing"),
                "timeoutCount", pickingOrderRepository.countByCommandStatus("COMM_TIMEOUT")
        ));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(@RequestParam(defaultValue = "50") int limit) {
        List<Map<String, Object>> rows = pickingOrderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 200)))
                .stream()
                .map(this::toOrderRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/logs")
    public ResponseEntity<?> logs(@RequestParam(defaultValue = "100") int limit) {
        List<Map<String, Object>> rows = communicationLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(0, Math.min(Math.max(limit, 1), 300)))
                .stream()
                .filter(log -> {
                    String messageType = defaultString(log.getMessageType());
                    String protocol = defaultString(log.getProtocol());
                    return protocol.equalsIgnoreCase("TCP")
                            || protocol.equalsIgnoreCase("REST")
                            || messageType.contains("ACCUPICK")
                            || messageType.contains("WES");
                })
                .map(this::toLogRow)
                .toList();
        return ResponseEntity.ok(rows);
    }

    private Map<String, Object> toOrderRow(PickingOrder order) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("createdAt", order.getCreatedAt());
        row.put("updatedAt", order.getUpdatedAt());
        row.put("commandNo", order.getCommandNo());
        row.put("orderNo", order.getOrderNo());
        row.put("orderlineNo", order.getOrderlineNo());
        row.put("dcId", order.getDcId());
        row.put("workstationId", order.getWorkstationId());
        row.put("inboundCarrierId", order.getInboundCarrierId());
        row.put("productId", order.getProductId());
        row.put("onHandQty", order.getOnHandQty());
        row.put("pickingQty", order.getPickingQty());
        row.put("outboundCarrierId", order.getOutboundCarrierId());
        row.put("commandControlCode", order.getCommandControlCode());
        row.put("commandStatus", order.getCommandStatus());
        row.put("actualQty", order.getActualQty());
        row.put("abnormalReasonCode", order.getAbnormalReasonCode());
        row.put("commandTime", order.getCommandTime());
        row.put("jobNo", order.getJobNo());
        row.put("jobStatus", order.getJobStatus());
        row.put("ngCode", order.getNgCode());
        row.put("comment", order.getComment());
        row.put("sourcePayload", order.getSourcePayload());
        row.put("ackPayload", order.getAckPayload());
        return row;
    }

    private Map<String, Object> toLogRow(CommunicationLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("timestamp", log.getTimestamp());
        row.put("direction", log.getDirection());
        row.put("protocol", log.getProtocol());
        row.put("messageType", log.getMessageType());
        row.put("jobNo", log.getJobNo());
        row.put("carrierId", log.getCarrierId());
        row.put("remoteAddress", log.getRemoteAddress());
        row.put("content", log.getContent());
        return row;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
