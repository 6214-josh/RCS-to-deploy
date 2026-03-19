package com.rcs.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequeueCallbackResponse {
    @JsonProperty("command_no")
    private String commandNo;
    @JsonProperty("original_command_no")
    private String originalCommandNo;
    @JsonProperty("command_status")
    private String commandStatus;
    @JsonProperty("actual_qty")
    private Integer actualQty;
    @JsonProperty("abnormal_reason_code")
    private Integer abnormalReasonCode;
}
