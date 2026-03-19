package com.rcs.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummary {
    private long totalOrders;
    private long inQueue;
    private long processing;
    private long success;
    private long ngErrors;
    private String accupickMode;
    private String dashboardWebsocket;
}
