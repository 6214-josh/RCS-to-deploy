package com.rcs.system.wes;

import org.springframework.stereotype.Component;

@Component
public class WesSocketMessageParser {

    public WesSocketMessage parse(String rawMessage, String remoteAddress) {
        String[] parts = rawMessage.split("\\|", -1);
        WesSocketMessage message = new WesSocketMessage();
        message.setRawMessage(rawMessage);
        message.setRemoteAddress(remoteAddress);

        if (parts.length > 0) {
            message.setSource(parts[0]);
        }
        if (parts.length >= 7) {
            message.setSceneCode(parts[1]);
            message.setSceneDescription(parts[2]);
            message.setInbound(parts[3]);
            message.setProductId(parts[4]);
            message.setProductName(parts[5]);
            message.setQuantity(parseInteger(parts[6]));
        }
        if (parts.length >= 12) {
            message.setNormalCompleteCode(parts[7]);
            message.setNormalCompleteQty(parseInteger(parts[8]));
            message.setAbnormalCompleteCode(parts[9]);
            message.setAbnormalReasonCode(emptyToNull(parts[10]));
            message.setAbnormalCompleteQty(parseInteger(parts[11]));
        }
        return message;
    }

    private Integer parseInteger(String value) {
        try {
            if (value == null || value.isBlank() || "NULL".equalsIgnoreCase(value)) {
                return null;
            }
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank() || "NULL".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }
}
