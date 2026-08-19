package com.infosys.transaction_service.util;

import com.infosys.transaction_service.exception.AccessDeniedException;
import com.infosys.transaction_service.feign.AuthFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthUtil {

    @Autowired
    AuthFeignClient authFeignClient;

    public Map<String, Object> validateAndGetClaims(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        Map<String, Object> result = authFeignClient.validate(token);

        if (!Boolean.TRUE.equals(result.get("valid"))) {
            throw new RuntimeException("Invalid or expired token");
        }
        return result;
    }

    public void requireOwnershipOrTeller(String authHeader, Integer accountOwnerCustId) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");

        if ("TELLER".equals(role) || "MANAGER".equals(role)) {
            return;
        }

        if ("CUSTOMER".equals(role)) {
            Object custIdClaim = claims.get("custId");
            Integer callerCustId = custIdClaim != null ? ((Number) custIdClaim).intValue() : null;
            if (callerCustId == null || !callerCustId.equals(accountOwnerCustId)) {
                throw new AccessDeniedException("Access denied. You do not own this account.");
            }
            return;
        }

        throw new AccessDeniedException("Access denied. Unrecognized role: " + role);
    }

    public void requireTeller(String authHeader) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");
        if (!"TELLER".equals(role) && !"MANAGER".equals(role)) {
            throw new com.infosys.transaction_service.exception.AccessDeniedException("Access denied. Staff only.");
        }
    }
}