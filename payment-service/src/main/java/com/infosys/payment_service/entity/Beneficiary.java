package com.infosys.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer beneficiaryId;
    private Integer custId;
    private String name;
    private Integer accountNumber; // maps to accId in Account Service
    private String nickname;
    private LocalDateTime addedAt;
}