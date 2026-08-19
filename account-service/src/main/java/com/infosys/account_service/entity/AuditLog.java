package com.infosys.account_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer auditId;
    private String action;      // e.g. "ACCOUNT_CREATED", "ACCOUNT_FROZEN"
    private Integer accId;
    private String details;
    private LocalDateTime timestamp;
}