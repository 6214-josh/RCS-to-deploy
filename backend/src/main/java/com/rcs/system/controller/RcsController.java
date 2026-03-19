package com.rcs.system.controller;

import com.rcs.system.dto.CommandAckResponse;
import com.rcs.system.dto.MockAckRequest;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.UserRepository;
import com.rcs.system.service.DashboardQueryService;
import com.rcs.system.service.OrderCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "RCS Frozen Sorting API", description = "REST + WebSocket dashboard API aligned to the SDD DOCX")
public class RcsController {

    private final OrderCommandService orderCommandService;
    private final DashboardQueryService dashboardQueryService;
    private final UserRepository userRepository;

    @PostMapping("/api/auth/login")
    @Operation(summary = "Simple login for local UI")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(Map.of("status", "success", "user", user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Invalid credentials")));
    }

    @PostMapping("/api/v1/mock/commands/{commandNo}/ack")
    @Operation(summary = "Manual/mock ACK injector for local demo")
    public ResponseEntity<?> mockAck(@PathVariable String commandNo, @RequestBody MockAckRequest request) {
        try {
            PickingOrder order = orderCommandService.applyManualMockAck(commandNo, request);
            CommandAckResponse callback = orderCommandService.toCallbackResponse(order);
            return ResponseEntity.ok(Map.of("data", order, "callback", callback));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/v1/dashboard/summary")
    @Operation(summary = "Dashboard summary")
    public ResponseEntity<?> getSummary() {
        return ResponseEntity.ok(dashboardQueryService.getSummary(0));
    }

    @GetMapping("/api/v1/dashboard/orders")
    @Operation(summary = "Recent orders for dashboard")
    public ResponseEntity<?> getOrders(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(dashboardQueryService.getRecentOrders(limit));
    }

    @GetMapping("/api/v1/dashboard/queue")
    @Operation(summary = "Queue view")
    public ResponseEntity<?> getQueueOrders() {
        return ResponseEntity.ok(dashboardQueryService.getQueueOrders());
    }

    @GetMapping("/api/v1/dashboard/logs")
    @Operation(summary = "Recent communication logs")
    public ResponseEntity<?> getLogs(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(dashboardQueryService.getRecentLogs(limit));
    }

    @GetMapping("/api/v1/dashboard/db-status")
    @Operation(summary = "Database status")
    public ResponseEntity<?> getDbStatus() {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "Connected",
                    "orders", dashboardQueryService.getSummary(0).getTotalOrders(),
                    "users", userRepository.count()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "Error", "detail", e.getMessage()));
        }
    }
}
