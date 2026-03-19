package com.rcs.system.service;

import com.rcs.system.dashboard.DashboardWebSocketService;
import com.rcs.system.model.CommunicationLog;
import com.rcs.system.repository.CommunicationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunicationLogService {

    private final CommunicationLogRepository communicationLogRepository;
    private final DashboardWebSocketService dashboardWebSocketService;

    @Async("taskExecutor")
    public void saveLogAsync(String direction, String protocol, String messageType, String content, String jobNo, String carrierId, String remoteAddress) {
        CommunicationLog log = buildLog(direction, protocol, messageType, content, jobNo, carrierId, remoteAddress);
        communicationLogRepository.save(log);
        dashboardWebSocketService.publishLog(log);
    }

    public void saveLog(String direction, String protocol, String messageType, String content, String jobNo, String carrierId, String remoteAddress) {
        CommunicationLog log = buildLog(direction, protocol, messageType, content, jobNo, carrierId, remoteAddress);
        communicationLogRepository.save(log);
        dashboardWebSocketService.publishLog(log);
    }

    private CommunicationLog buildLog(String direction, String protocol, String messageType, String content, String jobNo, String carrierId, String remoteAddress) {
        CommunicationLog log = new CommunicationLog();
        log.setDirection(direction);
        log.setProtocol(protocol);
        log.setMessageType(messageType);
        log.setContent(content);
        log.setJobNo(jobNo);
        log.setCarrierId(carrierId);
        log.setRemoteAddress(remoteAddress);
        return log;
    }
}
