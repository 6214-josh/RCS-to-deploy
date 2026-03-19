package com.rcs.system.dto;

import lombok.Data;

@Data
public class MockAckRequest {
    private String commandStatus;
    private Integer actualQty;
    private Integer abnormalReasonCode;
    private Long commandTime;
    private String errorDetail;
}
