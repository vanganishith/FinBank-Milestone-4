package com.infosys.transaction_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sagaLogId;
    private Integer accId;
    private String step;
    private String status;
    private String detail;
    private LocalDateTime timestamp;
}