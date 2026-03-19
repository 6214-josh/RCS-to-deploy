package com.rcs.system.service;

import com.rcs.system.dto.WesCommandRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CommandValidationService {

    private static final Set<String> CONTROL_CODES = Set.of("NORMAL", "DBOX", "DBXX", "DBRS");

    public List<String> validate(WesCommandRequest request) {
        List<String> errors = new ArrayList<>();
        requireString(request.getOrderNo(), 32, "Order_No", errors, !isControlOnly(request));
        requireString(request.getOrderlineNo(), 8, "Orderline_No", errors, !isControlOnly(request));
        requireString(request.getDcId(), 8, "DC_ID", errors, !isControlOnly(request));
        requireString(request.getWorkStationId(), 16, "WorkStation_ID", errors, !isControlOnly(request));
        requireString(request.getInboundCarrierId(), 32, "Inbound_Carrier_ID", errors, !isControlOnly(request));
        requireString(request.getProductId(), 32, "Product_ID", errors, !isControlOnly(request));
        requireInteger(request.getOnHandQty(), 0, 9999, "On_hand_qty", errors, !isControlOnly(request));
        requireInteger(request.getPickingQty(), 1, 9999, "Picking_qty", errors, !isControlOnly(request));
        requireString(request.getOutboundCarrierId(), 32, "Outbound_Carrier_ID", errors, !isControlOnly(request));
        requireString(request.getCommandNo(), 48, "Command_No", errors, true);
        requireString(request.getCommandControlCode(), 16, "Command_Control_Code", errors, true);
        if (request.getCommandControlCode() != null && !request.getCommandControlCode().isBlank() && !CONTROL_CODES.contains(request.getCommandControlCode())) {
            errors.add("Command_Control_Code must be one of NORMAL/DBOX/DBXX/DBRS");
        }
        return errors;
    }

    public boolean isControlOnly(WesCommandRequest request) {
        String code = request.getCommandControlCode();
        return "DBXX".equals(code) || "DBRS".equals(code);
    }

    private void requireString(String value, int maxLength, String field, List<String> errors, boolean required) {
        if (!required) {
            return;
        }
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
            return;
        }
        if (value.length() > maxLength) {
            errors.add(field + " exceeds max length " + maxLength);
        }
    }

    private void requireInteger(Integer value, int min, int max, String field, List<String> errors, boolean required) {
        if (!required) {
            return;
        }
        if (value == null) {
            errors.add(field + " is required");
            return;
        }
        if (value < min || value > max) {
            errors.add(field + " must be between " + min + " and " + max);
        }
    }
}
