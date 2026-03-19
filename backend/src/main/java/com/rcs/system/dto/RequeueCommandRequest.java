package com.rcs.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequeueCommandRequest {
    @JsonProperty("command_no")
    private String commandNo;
    @JsonProperty("original_command_no")
    private String originalCommandNo;
    @JsonProperty("picking_qty")
    private Integer pickingQty;
    @JsonProperty("outbound_carrier_id")
    private String outboundCarrierId;
    @JsonProperty("reason")
    private String reason;
}
