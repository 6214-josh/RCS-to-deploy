package com.rcs.system.controller;

import com.rcs.system.dto.CommandAckRequest;
import com.rcs.system.dto.ControlCommandRequest;
import com.rcs.system.dto.PickCommandRequest;
import com.rcs.system.dto.RequeueCommandRequest;
import com.rcs.system.model.PickingOrder;
import com.rcs.system.service.OrderCommandService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1/commands")
public class WesCommandController {

    private final OrderCommandService orderCommandService;

    @PostMapping("/pick")
    @Operation(summary = "WES -> RCS normal pick")
    public ResponseEntity<?> pick(@RequestBody PickCommandRequest request) {
        try {
            return buildCreateResponse(orderCommandService.createPickCommand(request, false));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/pick/dbox")
    @Operation(summary = "WES -> RCS dbox pick")
    public ResponseEntity<?> pickDbox(@RequestBody PickCommandRequest request) {
        try {
            return buildCreateResponse(orderCommandService.createPickCommand(request, true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/requeue")
    @Operation(summary = "WES -> RCS requeue")
    public ResponseEntity<?> requeue(@RequestBody RequeueCommandRequest request) {
        try {
            return buildCreateResponse(orderCommandService.createRequeueCommand(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/control")
    @Operation(summary = "WES -> RCS control")
    public ResponseEntity<?> control(@RequestBody ControlCommandRequest request) {
        try {
            return buildCreateResponse(orderCommandService.createControlCommand(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{commandNo}")
    public ResponseEntity<?> get(@PathVariable String commandNo) {
        return orderCommandService.getByCommandNo(commandNo)
                .<ResponseEntity<?>>map(order -> ResponseEntity.ok(Map.of(
                        "data", order,
                        "callback", orderCommandService.toCallbackResponse(order)
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Command not found")));
    }

    @PostMapping("/{commandNo}/ack")
    public ResponseEntity<?> acknowledgeCallback(@PathVariable String commandNo,
                                                 @RequestBody(required = false) CommandAckRequest request) {
        try {
            PickingOrder order = orderCommandService.acknowledgeCallback(commandNo, request == null ? new CommandAckRequest() : request);
            return ResponseEntity.ok(Map.of("data", order, "status", "ACKED"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    private ResponseEntity<?> buildCreateResponse(OrderCommandService.CommandResult result) {
        return switch (result.status()) {
            case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Command accepted",
                    "data", result.order(),
                    "callback", orderCommandService.toCallbackResponse(result.order())
            ));
            case ACCEPTED -> ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                    "message", "Command already processing",
                    "data", result.order(),
                    "callback", orderCommandService.toCallbackResponse(result.order())
            ));
            case OK -> ResponseEntity.ok(Map.of(
                    "message", "Command already completed",
                    "data", result.order(),
                    "callback", orderCommandService.toCallbackResponse(result.order())
            ));
        };
    }
}
