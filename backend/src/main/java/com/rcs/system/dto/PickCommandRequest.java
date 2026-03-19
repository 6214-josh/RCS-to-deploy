package com.rcs.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PickCommandRequest {
    @JsonProperty("command_no")
    private String commandNo;
    @JsonProperty("order_no")
    private String orderNo;
    @JsonProperty("orderline_no")
    private String orderlineNo;
    @JsonProperty("dc_id")
    private String dcId;
    @JsonProperty("workstation_id")
    private String workstationId;
    @JsonProperty("inbound_carrier_id")
    private String inboundCarrierId;
    @JsonProperty("product_id")
    private String productId;
    @JsonProperty("on_hand_qty")
    private Integer onHandQty;
    @JsonProperty("picking_qty")
    private Integer pickingQty;
    @JsonProperty("outbound_carrier_id")
    private String outboundCarrierId;
}
