package com.infosys.account_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;
    private Integer accId;
    private String type;    // PAYMENT_SENT, PAYMENT_RECEIVED
    private String message;
    private Boolean read;
    private LocalDateTime createdAt;
}