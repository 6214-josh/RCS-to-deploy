package com.rcs.system.accupick;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Lazy
public class MockAccupickGateway implements AccupickGateway {

    @Value("${rcs.accupick.mock-enabled:true}")
    private boolean mockEnabled;

    @Override
    public AccupickAck sendCommand(String payload, int timeoutMs) {
        if (!mockEnabled) {
            throw new IllegalStateException("Mock gateway disabled");
        }

        Map<String, String> values = KvCsvUtil.decode(payload);
        String controlCode = values.getOrDefault("Command_Control_Code", "NORMAL");
        String commandNo = values.get("Command_No");
        Integer pickingQty = parseInteger(values.get("Picking_qty"));
        int actualQty = pickingQty == null ? 0 : pickingQty;

        if ("DBXX".equals(controlCode) || "DBRS".equals(controlCode)) {
            actualQty = 0;
        }

        return AccupickAck.builder()
                .commandNo(commandNo)
                .commandStatus("OK")
                .actualQty(("DBXX".equals(controlCode) || "DBRS".equals(controlCode)) ? null : actualQty)
                .abnormalReasonCode(0)
                .commandTime(1200L)
                .errorDetail(null)
                .rawMessage(buildAck(commandNo, controlCode, actualQty))
                .build();
    }

    @Override
    public String mode() {
        return mockEnabled ? "MOCK" : "DISABLED";
    }

    private Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAck(String commandNo, String controlCode, int actualQty) {
        if ("DBXX".equals(controlCode) || "DBRS".equals(controlCode)) {
            return String.format("Command_No,%s,Command_Status,OK", commandNo);
        }
        return String.format("Command_No,%s,Command_Status,OK,Actual_qty,%d,Abnormal_reason_code,0,Command_time,1200", commandNo, actualQty);
    }
}
