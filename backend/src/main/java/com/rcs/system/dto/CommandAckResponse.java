package com.rcs.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandAckResponse {
    private String commandNo;
    private String commandStatus;
    private Integer actualQty;
    private Integer abnormalReasonCode;
    private String commandControlCode;
}
