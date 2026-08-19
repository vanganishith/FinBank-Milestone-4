package com.infosys.customer_service.controller;

import java.util.HashMap;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.infosys.customer_service.dto.ClaimRequest;
import com.infosys.customer_service.dto.CustomerAuthInternal;
import com.infosys.customer_service.dto.KycSubmitRequest;
import com.infosys.customer_service.dto.RegisterRequest;
import com.infosys.customer_service.entity.AccountCreateRequest;
import com.infosys.customer_service.entity.Customer;
import com.infosys.customer_service.exception.DuplicateFieldException;
import com.infosys.customer_service.exception.InvalidRequestException;
import com.infosys.customer_service.feign.AccountFeignClient;
import com.infosys.customer_service.repository.CustomerRepo;
import com.infosys.customer_service.util.AuthUtil;

@RestController
@RequestMapping("/customer")
public class CustomerController {

    @Autowired
    CustomerRepo repo;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    AccountFeignClient accountFeignClient;

    @PostMapping("/kyc/submit")
    public Map<String, Object> submitKyc(@RequestBody KycSubmitRequest req, @RequestHeader("Authorization") String authHeader) {
        Integer custId = authUtil.requireCustomerAndGetId(authHeader);
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));

        if ("VERIFIED".equals(customer.getKycStatus())) {
            throw new InvalidRequestException("KYC is already verified. You cannot resubmit.");
        }
        if (req.getAccType() == null || !(req.getAccType().equals("SAVINGS") || req.getAccType().equals("CURRENT"))) {
            throw new InvalidRequestException("Account type must be SAVINGS or CURRENT.");
        }

        Map<String, Object> response = new HashMap<>();

        if (!isEligible(customer)) {
            customer.setKycStatus("REJECTED");
            customer.setKycRejectionReason("Automated check failed — please make sure your registered name is valid, then try again.");
            repo.save(customer);
            response.put("kycStatus", "REJECTED");
            response.put("reason", customer.getKycRejectionReason());
            return response;
        }

        customer.setKycStatus("VERIFIED");
        customer.setKycRejectionReason(null);
        repo.save(customer);

        accountFeignClient.createAccount(new AccountCreateRequest(custId, req.getAccType(), 0.0));

        response.put("kycStatus", "VERIFIED");
        response.put("message", "KYC verified — your " + req.getAccType() + " account has been created.");
        return response;
    }

    @GetMapping("/kyc/status")
    public Map<String, Object> kycStatus(@RequestHeader("Authorization") String authHeader) {
        Integer custId = authUtil.requireCustomerAndGetId(authHeader);
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
        Map<String, Object> response = new HashMap<>();
        response.put("kycStatus", customer.getKycStatus());
        response.put("rejectionReason", customer.getKycRejectionReason());
        response.put("canResubmit", customer.getKycStatus() == null || "REJECTED".equals(customer.getKycStatus()));
        return response;
    }

    private boolean isEligible(Customer c) {
        if (c.getName() == null || c.getName().trim().length() < 3) return false;
        if (c.getName().matches(".*\\d.*")) return false;
        return true; // email/phone already format-validated at registration
    }

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");

    private void validateContact(String email, String phone) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidRequestException("Invalid email format.");
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new InvalidRequestException("Phone number must be exactly 10 digits.");
        }
    }

    private void checkDuplicateContact(String email, String phone, Integer excludingCustId) {
        Customer byEmail = repo.findByEmail(email);
        if (byEmail != null && !byEmail.getCustId().equals(excludingCustId)) {
            throw new DuplicateFieldException("A customer with this email already exists.");
        }
        Customer byPhone = repo.findByPhone(phone);
        if (byPhone != null && !byPhone.getCustId().equals(excludingCustId)) {
            throw new DuplicateFieldException("A customer with this phone number already exists.");
        }
    }

    // ---- Teller-initiated (branch) customer creation — no login credentials yet ----
    @PostMapping("/add")
    public Customer addCustomer(@RequestBody Customer customer, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        validateContact(customer.getEmail(), customer.getPhone());
        checkDuplicateContact(customer.getEmail(), customer.getPhone(), null);

        customer.setKycStatus("PENDING");
        customer.setUsername(null);
        customer.setPassword(null);
        return repo.save(customer);
    }

    @GetMapping("/all")
    public List<Customer> getAllCustomers() {
        return (List<Customer>) repo.findAll();
    }

    @GetMapping("/{custId}")
    public Customer getCustomer(@PathVariable Integer custId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, custId);
        return repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    @PutMapping("/update")
    public Customer updateCustomer(@RequestBody Customer customer, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, customer.getCustId());
        validateContact(customer.getEmail(), customer.getPhone());
        checkDuplicateContact(customer.getEmail(), customer.getPhone(), customer.getCustId());

        Customer existing = repo.findById(customer.getCustId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        // KYC status, username, password are NOT updatable via this generic endpoint
        return repo.save(existing);
    }

    @DeleteMapping("/delete/{custId}")
    public String deleteCustomer(@PathVariable Integer custId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        repo.deleteById(custId);
        return "Customer deleted: " + custId;
    }

    @PutMapping("/kyc/verify/{custId}")
    public Customer verifyKyc(@PathVariable Integer custId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus("VERIFIED");
        customer.setKycRejectionReason(null);
        return repo.save(customer);
    }

    @PutMapping("/kyc/reject/{custId}")
    public Customer rejectKyc(@PathVariable Integer custId, @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus("REJECTED");
        customer.setKycRejectionReason(reason != null ? reason : "Not specified");
        return repo.save(customer);
    }

    // ---- Digital self-registration — creates a brand-new customer with login ----
    @PostMapping("/register")
    public Customer register(@RequestBody RegisterRequest req) {
        if (repo.findByUsername(req.getUsername()) != null) {
            throw new DuplicateFieldException("Username already taken.");
        }
        validateContact(req.getEmail(), req.getPhone());
        checkDuplicateContact(req.getEmail(), req.getPhone(), null);

        Customer customer = new Customer();
        customer.setName(req.getName());
        customer.setEmail(req.getEmail());
        customer.setPhone(req.getPhone());
        customer.setUsername(req.getUsername());
        customer.setPassword(encoder.encode(req.getPassword()));
        customer.setKycStatus("PENDING");

        return repo.save(customer);
    }

    // ---- Claim: for branch-created customers to attach login credentials ----
    @PostMapping("/claim")
    public Customer claim(@RequestBody ClaimRequest req) {
        Customer customer = repo.findById(req.getCustId())
                .orElseThrow(() -> new InvalidRequestException("No customer found with that customer ID."));

        if (customer.getUsername() != null) {
            throw new InvalidRequestException("This customer already has login credentials set up.");
        }
        if (repo.findByUsername(req.getUsername()) != null) {
            throw new DuplicateFieldException("Username already taken.");
        }

        customer.setUsername(req.getUsername());
        customer.setPassword(encoder.encode(req.getPassword()));
        return repo.save(customer);
    }

    @GetMapping("/internal/by-username/{username}")
    public CustomerAuthInternal getByUsernameInternal(@PathVariable String username) {
        Customer c = repo.findByUsername(username);
        if (c == null) return null;
        return new CustomerAuthInternal(c.getCustId(), c.getUsername(), c.getPassword(), c.getKycStatus());
    }

    // Internal use by other services — no auth required, no sensitive fields
    @GetMapping("/internal/{custId}")
    public Customer getCustomerInternal(@PathVariable Integer custId) {
        return repo.findById(custId).orElse(null);
    }

    // Internal use only — called by KYC Service after automated or manager-reviewed decision
    @PutMapping("/internal/kyc/verify/{custId}")
    public Customer verifyKycInternal(@PathVariable Integer custId) {
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus("VERIFIED");
        customer.setKycRejectionReason(null);
        return repo.save(customer);
    }

    @PutMapping("/internal/kyc/reject/{custId}")
    public Customer rejectKycInternal(@PathVariable Integer custId, @RequestParam(required = false) String reason) {
        Customer customer = repo.findById(custId).orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus("REJECTED");
        customer.setKycRejectionReason(reason != null ? reason : "Not specified");
        return repo.save(customer);
    }
}