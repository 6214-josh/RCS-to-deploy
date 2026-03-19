package com.rcs.system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ControlCommandRequest {
    @JsonProperty("command_no")
    private String commandNo;
    @JsonProperty("dc_id")
    private String dcId;
    @JsonProperty("workstation_id")
    private String workstationId;
    @JsonProperty("control_code")
    private String controlCode;
}
