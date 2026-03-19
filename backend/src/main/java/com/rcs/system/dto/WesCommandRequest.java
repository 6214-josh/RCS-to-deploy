package com.rcs.system.dto;

import lombok.Data;

@Data
public class WesCommandRequest {
    private String orderNo;
    private String orderlineNo;
    private String dcId;
    private String workStationId;
    private String inboundCarrierId;
    private String productId;
    private Integer onHandQty;
    private Integer pickingQty;
    private String outboundCarrierId;
    private String commandNo;
    private String commandControlCode;
}
