package com.rcs.system.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "rcs_logs")
public class RcsLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private String orderNo;
    private String commandNo;
    private String status;
    private String message;
    private LocalDateTime createdAt;
}
