package com.infosys.kyc_service.util;

import com.infosys.kyc_service.exception.AccessDeniedException;
import com.infosys.kyc_service.feign.AuthFeignClient;
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

    public void requireSelfOrTeller(String authHeader, Integer custId) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");
        if ("TELLER".equals(role) || "MANAGER".equals(role)) return;
        if ("CUSTOMER".equals(role)) {
            Object custIdClaim = claims.get("custId");
            Integer callerCustId = custIdClaim != null ? ((Number) custIdClaim).intValue() : null;
            if (callerCustId == null || !callerCustId.equals(custId)) {
                throw new AccessDeniedException("Access denied. This is not your KYC record.");
            }
            return;
        }
        throw new AccessDeniedException("Access denied. Unrecognized role: " + role);
    }

    public void requireManager(String authHeader) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");
        if (!"MANAGER".equals(role)) {
            throw new AccessDeniedException("Access denied. Manager review required.");
        }
    }
}