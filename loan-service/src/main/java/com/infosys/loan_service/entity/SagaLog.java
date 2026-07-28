package com.infosys.loan_service.entity;

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
    private Integer loanId;
    private String step;       // e.g. CREDIT_ACCOUNT, ACTIVATE_LOAN, PUBLISH_EVENT
    private String status;     // STARTED, SUCCESS, FAILED, COMPENSATED
    private String detail;
    private LocalDateTime timestamp;
}