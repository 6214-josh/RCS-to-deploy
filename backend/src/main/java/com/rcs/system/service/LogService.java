package com.rcs.system.service;

import com.rcs.system.model.RcsLog;
import com.rcs.system.repository.RcsLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {
    private final RcsLogRepository repo;

    public void log(String type, String orderNo, String commandNo, String status, String message) {
        RcsLog log = new RcsLog();
        log.setType(type);
        log.setOrderNo(orderNo);
        log.setCommandNo(commandNo);
        log.setStatus(status);
        log.setMessage(message);
        log.setCreatedAt(LocalDateTime.now());
        repo.save(log);
    }

    public List<RcsLog> getLatest() {
        return repo.findTop100ByOrderByCreatedAtDesc();
    }
}
