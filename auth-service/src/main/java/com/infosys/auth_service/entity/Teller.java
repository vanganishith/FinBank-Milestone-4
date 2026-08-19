package com.infosys.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tellerId;

    @Column(unique = true)
    private String username;

    private String password;
    private String role; // TELLER, MANAGER, ADMIN
}