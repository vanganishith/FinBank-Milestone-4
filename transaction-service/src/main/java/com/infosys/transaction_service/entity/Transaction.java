package com.infosys.transaction_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer txnId;
    private Integer accId;
    private String type;       // DEPOSIT or WITHDRAW
    private Double amount;
    private LocalDateTime txnDate;
}