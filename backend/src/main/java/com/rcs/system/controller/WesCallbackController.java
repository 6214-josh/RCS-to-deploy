package com.rcs.system.controller;

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
@RequestMapping("/api/v1/callbacks")
public class WesCallbackController {

    private final OrderCommandService orderCommandService;

    @PostMapping("/pick")
    @Operation(summary = "RCS callback payload for /commands/pick")
    public ResponseEntity<?> pick(@RequestBody Map<String, Object> request) {
        return buildResponse("PICK", request);
    }

    @PostMapping("/dbox")
    @Operation(summary = "RCS callback payload for /commands/pick/dbox")
    public ResponseEntity<?> dbox(@RequestBody Map<String, Object> request) {
        return buildResponse("DBOX", request);
    }

    @PostMapping("/requeue")
    @Operation(summary = "RCS callback payload for /commands/requeue")
    public ResponseEntity<?> requeue(@RequestBody Map<String, Object> request) {
        return buildResponse("REQUEUE", request);
    }

    @PostMapping("/control")
    @Operation(summary = "RCS callback payload for /commands/control")
    public ResponseEntity<?> control(@RequestBody Map<String, Object> request) {
        return buildResponse("CONTROL", request);
    }

    private ResponseEntity<?> buildResponse(String callbackType, Map<String, Object> request) {
        try {
            String commandNo = String.valueOf(request.getOrDefault("command_no", "")).trim();
            if (commandNo.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "command_no is required"));
            }
            return ResponseEntity.ok(Map.of(
                    "message", "Callback payload resolved from picking_orders",
                    "callback_type", callbackType,
                    "data", orderCommandService.getCallbackPayload(callbackType, commandNo)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }
}
