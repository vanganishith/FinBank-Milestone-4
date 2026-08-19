package com.infosys.customer_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "phone"),
        @UniqueConstraint(columnNames = "username")
})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer custId;
    private String name;
    private String email;
    private String phone;
    private String kycStatus;
    private String kycRejectionReason;

    private String username; // null for teller-created customers until they self-register or claim

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}