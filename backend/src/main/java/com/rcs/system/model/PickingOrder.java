package com.rcs.system.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "picking_orders")
public class PickingOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_no", length = 48, unique = true)
    private String commandNo;

    @Column(name = "order_no", length = 32)
    private String orderNo;

    @Column(name = "orderline_no", length = 8)
    private String orderlineNo;

    @Column(name = "dc_id", length = 8)
    private String dcId;

    @Column(name = "workstation_id", length = 16)
    private String workstationId;

    @Column(name = "inbound_carrier_id", length = 32)
    private String inboundCarrierId;

    @Column(name = "product_id", length = 32)
    private String productId;

    @Column(name = "on_hand_qty")
    private Integer onHandQty;

    @Column(name = "picking_qty")
    private Integer pickingQty;

    @Column(name = "outbound_carrier_id", length = 32)
    private String outboundCarrierId;

    @Column(name = "command_control_code", length = 16)
    private String commandControlCode;

    @Column(name = "command_status", length = 16)
    private String commandStatus; // Pending, Processing, OK, NG, COMM_TIMEOUT

    @Column(name = "actual_qty")
    private Integer actualQty;

    @Column(name = "abnormal_reason_code")
    private Integer abnormalReasonCode;

    @Column(name = "command_time")
    private Long commandTime;

    @Column(name = "error_detail", length = 64)
    private String errorDetail;

    @Column(name = "callback_acknowledged")
    private Boolean callbackAcknowledged = Boolean.FALSE;

    @Column(name = "job_no", length = 100)
    private String jobNo;

    @Column(name = "job_status", length = 100)
    private String jobStatus;

    @Column(name = "ng_code", length = 50)
    private String ngCode;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "line_id", length = 50)
    private String lineId;

    @Column(name = "robot_id", length = 50)
    private String robotId;

    @Column(name = "source_payload", columnDefinition = "TEXT")
    private String sourcePayload;

    @Column(name = "ack_payload", columnDefinition = "TEXT")
    private String ackPayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    public String getCarrierId() {
        return inboundCarrierId;
    }

    public void setCarrierId(String carrierId) {
        this.inboundCarrierId = carrierId;
    }

    @JsonIgnore
    public Integer getOrderQty() {
        return pickingQty;
    }

    public void setOrderQty(Integer orderQty) {
        this.pickingQty = orderQty;
    }
}
