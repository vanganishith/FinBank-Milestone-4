package com.infosys.customer_service.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String phone;
    private String username;
    private String password;
}