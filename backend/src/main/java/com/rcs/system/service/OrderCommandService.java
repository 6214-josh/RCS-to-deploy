package com.rcs.system.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rcs.system.accupick.AccupickAck;
import com.rcs.system.accupick.AccupickGateway;
import com.rcs.system.accupick.KvCsvUtil;
import com.rcs.system.dashboard.DashboardWebSocketService;
import com.rcs.system.dto.*;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.PickingOrderRepository;
import com.rcs.system.wes.WesCallbackClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    public enum CommandStatusType { CREATED, ACCEPTED, OK }
    public record CommandResult(CommandStatusType status, PickingOrder order) {}
    private record CallbackTarget(String callbackType, String originalCommandNo) {}

    private final PickingOrderRepository pickingOrderRepository;
    private final CommandValidationService commandValidationService;
    private final AccupickGateway accupickGateway;
    private final CommunicationLogService communicationLogService;
    private final DashboardWebSocketService dashboardWebSocketService;
    private final ObjectMapper objectMapper;
    private final WesCallbackClient wesCallbackClient;

    @Value("${rcs.accupick.timeout-ms:5000}")
    private int accupickTimeoutMs;

    public CommandResult createPickCommand(PickCommandRequest request, boolean dbox) {
        Optional<PickingOrder> existing = pickingOrderRepository.findByCommandNo(request.getCommandNo());
        if (existing.isPresent()) return existingResult(existing.get());

        WesCommandRequest validationRequest = new WesCommandRequest();
        validationRequest.setCommandNo(request.getCommandNo());
        validationRequest.setOrderNo(request.getOrderNo());
        validationRequest.setOrderlineNo(request.getOrderlineNo());
        validationRequest.setDcId(request.getDcId());
        validationRequest.setWorkStationId(request.getWorkstationId());
        validationRequest.setInboundCarrierId(request.getInboundCarrierId());
        validationRequest.setProductId(request.getProductId());
        validationRequest.setOnHandQty(request.getOnHandQty());
        validationRequest.setPickingQty(request.getPickingQty());
        validationRequest.setOutboundCarrierId(request.getOutboundCarrierId());
        validationRequest.setCommandControlCode(dbox ? "DBOX" : "NORMAL");
        List<String> errors = commandValidationService.validate(validationRequest);
        if (!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ", errors));

        PickingOrder order = new PickingOrder();
        order.setCommandNo(request.getCommandNo());
        order.setOrderNo(request.getOrderNo());
        order.setOrderlineNo(request.getOrderlineNo());
        order.setDcId(request.getDcId());
        order.setWorkstationId(request.getWorkstationId());
        order.setInboundCarrierId(request.getInboundCarrierId());
        order.setProductId(request.getProductId());
        order.setOnHandQty(request.getOnHandQty());
        order.setPickingQty(request.getPickingQty());
        order.setOutboundCarrierId(request.getOutboundCarrierId());
        order.setCommandControlCode(dbox ? "DBOX" : "NORMAL");
        order.setSourcePayload(writeJson(request));
        order.setOrderQty(request.getPickingQty());
        order.setComment("WES REST command accepted");
        return createAndDispatch(order, dbox ? "DBOX" : "PICK", null);
    }

    public CommandResult createControlCommand(ControlCommandRequest request) {
        Optional<PickingOrder> existing = pickingOrderRepository.findByCommandNo(request.getCommandNo());
        if (existing.isPresent()) return existingResult(existing.get());
        if (request.getCommandNo() == null || request.getCommandNo().isBlank()) throw new IllegalArgumentException("command_no is required");
        if (request.getDcId() == null || request.getDcId().isBlank()) throw new IllegalArgumentException("dc_id is required");
        if (request.getWorkstationId() == null || request.getWorkstationId().isBlank()) throw new IllegalArgumentException("workstation_id is required");
        if (request.getControlCode() == null || request.getControlCode().isBlank()) throw new IllegalArgumentException("control_code is required");

        PickingOrder order = new PickingOrder();
        order.setCommandNo(request.getCommandNo());
        order.setDcId(request.getDcId());
        order.setWorkstationId(request.getWorkstationId());
        order.setCommandControlCode(request.getControlCode());
        order.setSourcePayload(writeJson(request));
        order.setComment("WES REST control accepted");
        return createAndDispatch(order, "CONTROL", null);
    }

    public CommandResult createRequeueCommand(RequeueCommandRequest request) {
        Optional<PickingOrder> existing = pickingOrderRepository.findByCommandNo(request.getCommandNo());
        if (existing.isPresent()) return existingResult(existing.get());
        if (request.getCommandNo() == null || request.getCommandNo().isBlank()) throw new IllegalArgumentException("command_no is required");
        if (request.getOriginalCommandNo() == null || request.getOriginalCommandNo().isBlank()) throw new IllegalArgumentException("original_command_no is required");
        PickingOrder original = pickingOrderRepository.findByCommandNo(request.getOriginalCommandNo())
                .orElseThrow(() -> new IllegalArgumentException("original_command_no not found: " + request.getOriginalCommandNo()));

        PickingOrder order = new PickingOrder();
        order.setCommandNo(request.getCommandNo());
        order.setOrderNo(original.getOrderNo());
        order.setOrderlineNo(original.getOrderlineNo());
        order.setDcId(original.getDcId());
        order.setWorkstationId(original.getWorkstationId());
        order.setInboundCarrierId(original.getInboundCarrierId());
        order.setProductId(original.getProductId());
        order.setOnHandQty(original.getOnHandQty());
        order.setPickingQty(request.getPickingQty());
        order.setOutboundCarrierId(request.getOutboundCarrierId());
        order.setCommandControlCode("NORMAL");
        order.setSourcePayload(writeJson(request));
        order.setComment("REQUEUE reason=" + trim(request.getReason(), 64));
        return createAndDispatch(order, "REQUEUE", request.getOriginalCommandNo());
    }

    private CommandResult createAndDispatch(PickingOrder order, String callbackType, String originalCommandNo) {
        order.setCommandStatus("Pending");
        order.setActualQty(null);
        order.setAbnormalReasonCode(0);
        order.setJobNo("JOB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setJobStatus("Queued");
        pickingOrderRepository.save(order);
        dashboardWebSocketService.publishOrder(order);

        String cmdPayload = buildCmdPayload(order);
        communicationLogService.saveLog("IN", "REST", "WES_COMMAND", order.getSourcePayload(), order.getJobNo(), order.getOutboundCarrierId(), "WES");
        communicationLogService.saveLog("OUT", "TCP", "ACCUPICK_CMD", cmdPayload, order.getJobNo(), order.getOutboundCarrierId(), "AccuPick");
        order.setCommandStatus("Processing");
        order.setJobStatus("Sent to AccuPick");
        pickingOrderRepository.save(order);
        dashboardWebSocketService.publishOrder(order);

        try {
            AccupickAck ack = accupickGateway.sendCommand(cmdPayload, accupickTimeoutMs);
            if (ack != null) {
                PickingOrder updated = applyAck(order.getCommandNo(), ack);
                sendCallbackToWes(updated, callbackType, originalCommandNo);
                return new CommandResult(CommandStatusType.CREATED, updated);
            }
            order.setComment("CMD sent to AccuPick; waiting async ACK");
            pickingOrderRepository.save(order);
            dashboardWebSocketService.publishOrder(order);
            return new CommandResult(CommandStatusType.CREATED, order);
        } catch (RuntimeException e) {
            order.setCommandStatus("COMM_TIMEOUT");
            order.setJobStatus("AccuPick unavailable");
            order.setAbnormalReasonCode(4);
            order.setErrorDetail(trim(e.getMessage(), 64));
            order.setNgCode("COMM_TIMEOUT");
            order.setComment("RCS↔AccuPick TCP communication failed");
            pickingOrderRepository.save(order);
            communicationLogService.saveLog("ERROR", "TCP", "ACCUPICK_TIMEOUT", e.getMessage(), order.getJobNo(), order.getOutboundCarrierId(), "AccuPick");
            dashboardWebSocketService.publishOrder(order);
            throw e;
        }
    }

    private void sendCallbackToWes(PickingOrder order, String callbackType, String originalCommandNo) {
        Object body = switch (callbackType) {
            case "CONTROL" -> Map.of(
                    "command_no", order.getCommandNo(),
                    "control_code", defaultControlCode(order.getCommandControlCode()),
                    "command_status", mapStatusForCallback(order.getCommandStatus())
            );
            case "REQUEUE" -> new RequeueCallbackResponse(
                    order.getCommandNo(),
                    originalCommandNo,
                    mapStatusForCallback(order.getCommandStatus()),
                    order.getActualQty(),
                    order.getAbnormalReasonCode() == null ? 0 : order.getAbnormalReasonCode());
            case "DBOX" -> toJsonMap(toCallbackResponse(order));
            default -> toJsonMap(toCallbackResponse(order));
        };
        String path = switch (callbackType) {
            case "CONTROL" -> "/callbacks/control";
            case "REQUEUE" -> "/callbacks/requeue";
            case "DBOX" -> "/callbacks/dbox";
            default -> "/callbacks/pick";
        };
        wesCallbackClient.post(path, body);
        communicationLogService.saveLog("OUT", "REST", "WES_CALLBACK", String.valueOf(body), order.getJobNo(), order.getOutboundCarrierId(), "WES");
    }

    private Map<String, Object> toJsonMap(CommandAckResponse response) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("command_no", response.getCommandNo());
        map.put("command_status", response.getCommandStatus());
        map.put("actual_qty", response.getActualQty());
        map.put("abnormal_reason_code", response.getAbnormalReasonCode());
        map.put("command_control_code", response.getCommandControlCode());
        return map;
    }

    private CommandResult existingResult(PickingOrder order) {
        if ("Pending".equalsIgnoreCase(order.getCommandStatus()) || "Processing".equalsIgnoreCase(order.getCommandStatus())) {
            return new CommandResult(CommandStatusType.ACCEPTED, order);
        }
        return new CommandResult(CommandStatusType.OK, order);
    }

    public Optional<PickingOrder> getByCommandNo(String commandNo) {
        return pickingOrderRepository.findByCommandNo(commandNo);
    }

    public PickingOrder acknowledgeCallback(String commandNo, CommandAckRequest request) {
        PickingOrder order = pickingOrderRepository.findByCommandNo(commandNo)
                .orElseThrow(() -> new IllegalArgumentException("Command not found: " + commandNo));
        order.setCallbackAcknowledged(request.getAcknowledged() == null || request.getAcknowledged());
        order.setComment("WES callback acknowledged");
        pickingOrderRepository.save(order);
        dashboardWebSocketService.publishOrder(order);
        return order;
    }

    public PickingOrder handleAsyncAck(AccupickAck ack) {
        if (ack == null || ack.getCommandNo() == null || ack.getCommandNo().isBlank()) {
            throw new IllegalArgumentException("ACK command_no is required");
        }
        PickingOrder updated = applyAck(ack.getCommandNo(), ack);
        CallbackTarget callbackTarget = resolveCallbackTarget(updated);
        sendCallbackToWes(updated, callbackTarget.callbackType(), callbackTarget.originalCommandNo());
        return updated;
    }
    private CallbackTarget resolveCallbackTarget(PickingOrder order) {
        if (isControlOnly(order.getCommandControlCode())) {
            return new CallbackTarget("CONTROL", null);
        }
        if ("DBOX".equalsIgnoreCase(defaultControlCode(order.getCommandControlCode()))) {
            return new CallbackTarget("DBOX", null);
        }
        String sourcePayload = order.getSourcePayload();
        if (sourcePayload != null && sourcePayload.contains("originalCommandNo")) {
            return new CallbackTarget("REQUEUE", extractJsonString(sourcePayload, "originalCommandNo"));
        }
        if (sourcePayload != null && sourcePayload.contains("original_command_no")) {
            return new CallbackTarget("REQUEUE", extractJsonString(sourcePayload, "original_command_no"));
        }
        return new CallbackTarget("PICK", null);
    }
    private String extractJsonString(String json, String fieldName) {
        try {
            return objectMapper.readTree(json).path(fieldName).asText(null);
        } catch (Exception e) {
            return null;
        }
    }
    public PickingOrder applyManualMockAck(String commandNo, MockAckRequest request) {
        AccupickAck ack = AccupickAck.builder()
                .commandNo(commandNo)
                .commandStatus(request.getCommandStatus() == null ? "NG" : request.getCommandStatus())
                .actualQty(request.getActualQty())
                .abnormalReasonCode(request.getAbnormalReasonCode())
                .commandTime(request.getCommandTime())
                .errorDetail(request.getErrorDetail())
                .rawMessage(buildManualAckRaw(commandNo, request))
                .build();
        return applyAck(commandNo, ack);
    }

    public PickingOrder applyAck(String commandNo, AccupickAck ack) {
        PickingOrder order = pickingOrderRepository.findByCommandNo(commandNo)
                .orElseThrow(() -> new IllegalArgumentException("Command not found: " + commandNo));

        order.setAckPayload(ack.getRawMessage());
        order.setCommandTime(ack.getCommandTime());
        order.setErrorDetail(trim(ack.getErrorDetail(), 64));
        order.setNgCode(String.valueOf(ack.getAbnormalReasonCode() == null ? 0 : ack.getAbnormalReasonCode()));
        order.setActualQty(resolveActualQty(order, ack));
        order.setAbnormalReasonCode(ack.getAbnormalReasonCode() == null ? 0 : ack.getAbnormalReasonCode());
        order.setCommandStatus(normalizeStatus(ack.getCommandStatus()));
        order.setJobStatus("AccuPick ACK received");
        order.setComment(describeAck(order));
        pickingOrderRepository.save(order);

        communicationLogService.saveLog("IN", "TCP", "ACCUPICK_ACK", ack.getRawMessage(), order.getJobNo(), order.getOutboundCarrierId(), "AccuPick");
        dashboardWebSocketService.publishOrder(order);
        return order;
    }

    public String buildCmdPayload(PickingOrder order) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Command_No", order.getCommandNo());
        if (!isControlOnly(order.getCommandControlCode())) {
            values.put("Picking_qty", String.valueOf(order.getPickingQty()));
            values.put("Outbound_Carrier_ID", order.getOutboundCarrierId());
            values.put("Product_ID", order.getProductId());
            values.put("Inbound_Carrier_ID", order.getInboundCarrierId());
        }
        if (!"NORMAL".equals(defaultControlCode(order.getCommandControlCode()))) {
            values.put("Command_Control_Code", order.getCommandControlCode());
        }
        return KvCsvUtil.encode(values);
    }

    public Map<String, Object> getCallbackPayload(String callbackType, String commandNo) {
        PickingOrder order = pickingOrderRepository.findByCommandNo(commandNo)
                .orElseThrow(() -> new IllegalArgumentException("Command not found: " + commandNo));

        String normalizedType = callbackType == null ? "pick" : callbackType.trim().toLowerCase();
        return switch (normalizedType) {
            case "control" -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("command_no", order.getCommandNo());
                map.put("control_code", defaultControlCode(order.getCommandControlCode()));
                map.put("command_status", mapStatusForCallback(order.getCommandStatus()));
                yield map;
            }
            case "requeue" -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("command_no", order.getCommandNo());
                map.put("original_command_no", order.getOrderNo());
                map.put("command_status", mapStatusForCallback(order.getCommandStatus()));
                map.put("actual_qty", order.getActualQty());
                map.put("abnormal_reason_code", order.getAbnormalReasonCode() == null ? 0 : order.getAbnormalReasonCode());
                yield map;
            }
            case "dbox", "pick" -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("command_no", order.getCommandNo());
                map.put("command_status", mapStatusForCallback(order.getCommandStatus()));
                map.put("actual_qty", isControlOnly(order.getCommandControlCode()) ? null : order.getActualQty());
                map.put("abnormal_reason_code", order.getAbnormalReasonCode() == null ? 0 : order.getAbnormalReasonCode());
                yield map;
            }
            default -> throw new IllegalArgumentException("Unsupported callback_type: " + callbackType);
        };
    }

    public CommandAckResponse toCallbackResponse(PickingOrder order) {
        return new CommandAckResponse(
                order.getCommandNo(),
                mapStatusForCallback(order.getCommandStatus()),
                isControlOnly(order.getCommandControlCode()) ? null : order.getActualQty(),
                order.getAbnormalReasonCode() == null ? 0 : order.getAbnormalReasonCode(),
                defaultControlCode(order.getCommandControlCode())
        );
    }

    private String buildManualAckRaw(String commandNo, MockAckRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Command_No", commandNo);
        values.put("Command_Status", request.getCommandStatus() == null ? "NG" : request.getCommandStatus());
        if (request.getActualQty() != null) values.put("Actual_qty", String.valueOf(request.getActualQty()));
        if (request.getAbnormalReasonCode() != null) values.put("Abnormal_reason_code", String.valueOf(request.getAbnormalReasonCode()));
        if (request.getCommandTime() != null) values.put("Command_time", String.valueOf(request.getCommandTime()));
        if (request.getErrorDetail() != null && !request.getErrorDetail().isBlank()) values.put("Error_detail", trim(request.getErrorDetail(), 64));
        return KvCsvUtil.encode(values);
    }

    private String writeJson(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Integer resolveActualQty(PickingOrder order, AccupickAck ack) {
        if (isControlOnly(order.getCommandControlCode())) return null;
        if (ack.getActualQty() != null) return ack.getActualQty();
        if ("OK".equalsIgnoreCase(ack.getCommandStatus())) return order.getPickingQty();
        return 0;
    }

    private String mapStatusForCallback(String status) {
        return "OK".equalsIgnoreCase(status) ? "OK" : "NG";
    }

    private String normalizeStatus(String status) {
        return "OK".equalsIgnoreCase(status) ? "OK" : "NG";
    }

    private String defaultControlCode(String code) {
        return (code == null || code.isBlank()) ? "NORMAL" : code;
    }

    private boolean isControlOnly(String code) {
        return "DBXX".equals(code) || "DBRS".equals(code);
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String describeAck(PickingOrder order) {
        int abnormal = order.getAbnormalReasonCode() == null ? 0 : order.getAbnormalReasonCode();
        return switch (abnormal) {
            case 0 -> "AccuPick completed successfully";
            case 1 -> "SHORTAGE";
            case 2 -> "PICKING_FAIL";
            case 3 -> "OVERFLOW";
            case 4 -> "COMM_TIMEOUT";
            case 5 -> "CARRIER_NOT_FOUND";
            default -> "NG";
        };
    }
}
