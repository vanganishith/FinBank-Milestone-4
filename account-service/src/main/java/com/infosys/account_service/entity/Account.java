package com.infosys.account_service.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accId;
    private Integer custId; // links to a customer in Customer Service
    private String accType; // SAVINGS, CURRENT
    private Double balance;
    private String status; // ACTIVE, FROZEN, CLOSED

    @Transient // not persisted in DB, filled in dynamically via Feign call
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Customer customer;
}