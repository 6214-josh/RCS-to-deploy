package com.rcs.system.controller;

import com.rcs.system.model.RcsLog;
import com.rcs.system.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {
    private final LogService logService;

    @GetMapping
    public List<RcsLog> getLogs() {
        return logService.getLatest();
    }
}
