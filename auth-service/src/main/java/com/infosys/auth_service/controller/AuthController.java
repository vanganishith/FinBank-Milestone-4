package com.infosys.auth_service.controller;

import com.infosys.auth_service.entity.Teller;
import com.infosys.auth_service.repository.TellerRepo;
import com.infosys.auth_service.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.infosys.auth_service.dto.LoginRequest;
import com.infosys.auth_service.dto.RegisterRequest;
import com.infosys.auth_service.feign.CustomerAuthInternal;
import com.infosys.auth_service.feign.CustomerFeignClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    TellerRepo repo;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    CustomerFeignClient customerFeignClient;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final List<String> VALID_ROLES = List.of("TELLER", "MANAGER", "ADMIN");

    // ---- One-time bootstrap: works ONLY while zero staff accounts exist ----
    @PostMapping("/bootstrap-admin")
    public Map<String, Object> bootstrapAdmin(@RequestBody RegisterRequest req) {
        Map<String, Object> response = new HashMap<>();
        long existingStaff = repo.count();
        if (existingStaff > 0) {
            response.put("error", "Bootstrap already used. Ask an existing ADMIN to register new staff.");
            return response;
        }
        Teller admin = new Teller();
        admin.setUsername(req.getUsername());
        admin.setPassword(encoder.encode(req.getPassword()));
        admin.setRole("ADMIN");
        Teller saved = repo.save(admin);
        response.put("message", "First ADMIN account created. Bootstrap is now disabled.");
        response.put("username", saved.getUsername());
        return response;
    }

    // ---- ADMIN-only: create teller/manager/admin accounts ----
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req,
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> response = new HashMap<>();

        String callerToken = authHeader != null && authHeader.startsWith("Bearer ") ? authHeader.substring(7) : null;
        if (callerToken == null || !jwtUtil.isTokenValid(callerToken) || !"ADMIN".equals(jwtUtil.extractRole(callerToken))) {
            response.put("error", "Only an ADMIN can register new staff accounts.");
            return response;
        }

        if (!VALID_ROLES.contains(req.getRole())) {
            response.put("error", "Role must be one of: TELLER, MANAGER, ADMIN.");
            return response;
        }

        if (repo.findByUsername(req.getUsername()) != null) {
            response.put("error", "Username already taken.");
            return response;
        }

        Teller teller = new Teller();
        teller.setUsername(req.getUsername());
        teller.setPassword(encoder.encode(req.getPassword()));
        teller.setRole(req.getRole());
        Teller saved = repo.save(teller);

        response.put("username", saved.getUsername());
        response.put("role", saved.getRole());
        return response;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        Teller teller = repo.findByUsername(loginRequest.getUsername());
        Map<String, String> response = new HashMap<>();

        if (teller == null || !passwordMatchesAndMigrates(teller, loginRequest.getPassword())) {
            response.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String token = jwtUtil.generateToken(teller.getUsername(), teller.getRole());
        response.put("token", token);
        response.put("role", teller.getRole());
        response.put("username", teller.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Supports a one-time, safe migration of legacy development rows that were
     * inserted with a plaintext password. Successful legacy sign-ins are
     * immediately replaced with a BCrypt hash. New registrations are always
     * BCrypt-only.
     */
    private boolean passwordMatchesAndMigrates(Teller teller, String rawPassword) {
        if (rawPassword == null || teller.getPassword() == null) return false;
        String storedPassword = teller.getPassword();
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        if (!rawPassword.equals(storedPassword)) return false;
        teller.setPassword(encoder.encode(rawPassword));
        repo.save(teller);
        return true;
    }

    @GetMapping("/validate")
    public Map<String, Object> validate(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        boolean valid = jwtUtil.isTokenValid(token);
        response.put("valid", valid);
        if (valid) {
            response.put("username", jwtUtil.extractUsername(token));
            response.put("role", jwtUtil.extractRole(token));
            response.put("custId", jwtUtil.extractCustId(token));
        }
        return response;
    }

    @PostMapping("/customer-login")
    public Map<String, Object> customerLogin(@RequestBody LoginRequest loginRequest) {
        Map<String, Object> response = new HashMap<>();

        CustomerAuthInternal customer = customerFeignClient.getByUsername(loginRequest.getUsername());

        if (customer == null || customer.getPasswordHash() == null
                || !encoder.matches(loginRequest.getPassword(), customer.getPasswordHash())) {
            response.put("error", "Invalid username or password");
            return response;
        }

        String token = jwtUtil.generateCustomerToken(customer.getUsername(), customer.getCustId());
        response.put("token", token);
        response.put("role", "CUSTOMER");
        response.put("username", customer.getUsername());
        response.put("custId", customer.getCustId());
        response.put("kycStatus", customer.getKycStatus());
        return response;
    }
}
