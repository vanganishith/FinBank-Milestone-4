package com.infosys.payment_service.service;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class SettlementService {

    public String generateUtr(String method) {
        return method.toUpperCase() + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}