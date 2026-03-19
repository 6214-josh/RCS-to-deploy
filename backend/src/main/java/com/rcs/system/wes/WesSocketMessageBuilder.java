package com.rcs.system.wes;

import com.rcs.system.model.PickingOrder;
import org.springframework.stereotype.Component;

@Component
public class WesSocketMessageBuilder {

    public String buildOrderMessage(PickingOrder order) {
        return String.join("|",
                "WES",
                nv(order.getCommandNo()),
                nv(order.getCommandStatus()),
                nv(order.getCarrierId()),
                nv(order.getProductId()),
                safeProductName(order),
                nv(order.getOrderQty())
        );
    }

    public String buildResultMessage(PickingOrder order) {
        return String.join("|",
                "RCS",
                nv(order.getCommandNo()),
                nv(order.getJobStatus()),
                nv(order.getCarrierId()),
                nv(order.getProductId()),
                safeProductName(order),
                nv(order.getOrderQty()),
                successCode(order),
                successQty(order),
                failureCode(order),
                nv(order.getNgCode()),
                failureQty(order)
        );
    }

    private String safeProductName(PickingOrder order) {
        return order.getComment() == null ? "" : order.getComment();
    }

    private String successCode(PickingOrder order) {
        if ("Success".equalsIgnoreCase(order.getCommandStatus()) || "DB01OK".equalsIgnoreCase(order.getCommandStatus()) || "DB02OK".equalsIgnoreCase(order.getCommandStatus())) {
            return order.getCommandStatus();
        }
        return "";
    }

    private String successQty(PickingOrder order) {
        if (successCode(order).isBlank()) {
            return "";
        }
        return nv(order.getOrderQty());
    }

    private String failureCode(PickingOrder order) {
        if (order.getNgCode() != null && !order.getNgCode().isBlank()) {
            if (order.getCommandNo() != null && !order.getCommandNo().isBlank()) {
                return order.getCommandNo() + "NG";
            }
            return "NG";
        }
        return "";
    }

    private String failureQty(PickingOrder order) {
        return failureCode(order).isBlank() ? "" : nv(order.getOrderQty());
    }

    private String nv(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
