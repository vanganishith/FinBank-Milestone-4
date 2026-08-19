package com.infosys.account_service.util;

import com.infosys.account_service.feign.AuthFeignClient;
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

    public void requireRole(String authHeader, String requiredRole) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");
        if (!requiredRole.equals(role)) {
            throw new com.infosys.account_service.exception.AccessDeniedException(
                    "Access denied. Required role: " + requiredRole + ", but got: " + role);
        }
    }

    /**
     * Allows TELLER/MANAGER to act on any account.
     * If the caller is a CUSTOMER, their custId claim must match the account's
     * owner.
     */
    public void requireOwnershipOrTeller(String authHeader, Integer accountOwnerCustId) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");

        if ("TELLER".equals(role) || "MANAGER".equals(role)) {
            return; // staff can act on any account
        }

        if ("CUSTOMER".equals(role)) {
            Object custIdClaim = claims.get("custId");
            Integer callerCustId = custIdClaim != null ? ((Number) custIdClaim).intValue() : null;
            if (callerCustId == null || !callerCustId.equals(accountOwnerCustId)) {
                throw new com.infosys.account_service.exception.AccessDeniedException(
                        "Access denied. You do not own this account.");
            }
            return;
        }

        throw new com.infosys.account_service.exception.AccessDeniedException(
                "Access denied. Unrecognized role: " + role);
    }

    public void requireTeller(String authHeader) {
        Map<String, Object> claims = validateAndGetClaims(authHeader);
        String role = (String) claims.get("role");
        if (!"TELLER".equals(role) && !"MANAGER".equals(role)) {
            throw new com.infosys.account_service.exception.AccessDeniedException("Access denied. Staff only.");
        }
    }
}