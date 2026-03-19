package com.rcs.system.accupick;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccupickAck {
    private String commandNo;
    private String commandStatus;
    private Integer actualQty;
    private Integer abnormalReasonCode;
    private Long commandTime;
    private String errorDetail;
    private String rawMessage;
}
