package com.infosys.account_service.controller;

import com.infosys.account_service.entity.Account;
import com.infosys.account_service.entity.AuditLog;
import com.infosys.account_service.entity.Customer;
import com.infosys.account_service.exception.InvalidRequestException;
import com.infosys.account_service.feign.CustomerFeignClient;
import com.infosys.account_service.repository.AccountRepo;
import com.infosys.account_service.repository.AuditLogRepo;
import com.infosys.account_service.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.infosys.account_service.entity.Notification;
import com.infosys.account_service.repository.NotificationRepo;
import com.infosys.account_service.util.AuthUtil;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/account")
public class AccountController {

    @Autowired
    AccountRepo repo;

    @Autowired
    AccountService service;

    @Autowired
    AuditLogRepo auditRepo;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    NotificationRepo notificationRepo;

    @Autowired
    CustomerFeignClient customerFeignClient;

    private void log(String action, Integer accId, String details) {
        auditRepo.save(new AuditLog(null, action, accId, details, LocalDateTime.now()));
    }

    @PostMapping("/add")
    public Account addAccount(@RequestBody Account account, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);

        if (!"SAVINGS".equals(account.getAccType()) && !"CURRENT".equals(account.getAccType())) {
            throw new InvalidRequestException("Account type must be SAVINGS or CURRENT.");
        }

        Customer customer = customerFeignClient.getCustomer(account.getCustId());
        if (customer == null) {
            throw new InvalidRequestException("Customer not found.");
        }
        if (!"VERIFIED".equals(customer.getKycStatus())) {
            throw new InvalidRequestException(
                    "Cannot open account. Customer KYC status is " + customer.getKycStatus() + ", must be VERIFIED.");
        }

        Account existing = repo.findByCustIdAndAccType(account.getCustId(), account.getAccType());
        if (existing != null) {
            throw new InvalidRequestException(
                    "Customer already has a " + account.getAccType() + " account (accId " + existing.getAccId() + ").");
        }

        account.setStatus("ACTIVE");
        Account saved = repo.save(account);
        log("ACCOUNT_CREATED", saved.getAccId(), "Initial balance: " + saved.getBalance());
        return saved;
    }

    @GetMapping("/all")
    public List<Account> getAllAccounts(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return (List<Account>) repo.findAll();
    }

    @GetMapping("/{accId}")
    public Account getAccount(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return account;
    }

    @GetMapping("/withCustomer/{accId}")
    public Account getAccountWithCustomerDetails(@PathVariable Integer accId,
            @RequestHeader("Authorization") String authHeader) {
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return service.getAccountWithCustomer(accId);
    }

    // Internal use only — no auth check. Called exclusively by other microservices (Transaction, Payment, Loan sagas)
    // to update balance/status as part of orchestrated flows. Never expose this to direct client use.
    @PutMapping("/update")
    public Account updateAccount(@RequestBody Account account) {
        return repo.save(account);
    }

    @DeleteMapping("/delete/{accId}")
    public String deleteAccount(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireRole(authHeader, "MANAGER");
        repo.deleteById(accId);
        log("ACCOUNT_DELETED", accId, "Account removed");
        return "Account deleted: " + accId;
    }

    @PutMapping("/freeze/{accId}")
    public Account freezeAccount(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireRole(authHeader, "MANAGER");
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus("FROZEN");
        Account saved = repo.save(account);
        log("ACCOUNT_FROZEN", accId, "Account frozen");
        return saved;
    }

    @PutMapping("/close/{accId}")
    public Account closeAccount(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireRole(authHeader, "MANAGER");
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus("CLOSED");
        Account saved = repo.save(account);
        log("ACCOUNT_CLOSED", accId, "Account closed");
        return saved;
    }

    @PutMapping("/reactivate/{accId}")
    public Account reactivateAccount(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireRole(authHeader, "MANAGER");
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        account.setStatus("ACTIVE");
        Account saved = repo.save(account);
        log("ACCOUNT_REACTIVATED", accId, "Account reactivated");
        return saved;
    }

    @GetMapping("/audit/{accId}")
    public List<AuditLog> getAuditLog(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return auditRepo.findByAccId(accId);
    }

    // Internal use only — no auth check, called by other microservices via Feign
    @GetMapping("/internal/{accId}")
    public Account getAccountInternal(@PathVariable Integer accId) {
        return repo.findById(accId).orElse(null);
    }

    @GetMapping("/notifications/{accId}")
    public List<Notification> getNotifications(@PathVariable Integer accId,
            @RequestHeader("Authorization") String authHeader) {
        Account account = repo.findById(accId).orElseThrow(() -> new RuntimeException("Account not found"));
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return notificationRepo.findByAccIdOrderByCreatedAtDesc(accId);
    }

    @GetMapping("/customer/{custId}")
    public List<Account> getAccountsByCustomer(@PathVariable Integer custId,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, custId);
        return repo.findByCustId(custId);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        List<Account> all = (List<Account>) repo.findAll();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalAccounts", all.size());
        stats.put("activeAccounts", all.stream().filter(a -> "ACTIVE".equals(a.getStatus())).count());
        stats.put("totalBalance", all.stream().mapToDouble(a -> a.getBalance() != null ? a.getBalance() : 0).sum());
        return stats;
    }

    // Internal use only — no auth check, called by Customer Service after KYC auto-approval
    @PostMapping("/internal/create")
    public Account createAccountInternal(@RequestBody Account account) {
        if (!"SAVINGS".equals(account.getAccType()) && !"CURRENT".equals(account.getAccType())) {
            throw new InvalidRequestException("Account type must be SAVINGS or CURRENT.");
        }

        Account existing = repo.findByCustIdAndAccType(account.getCustId(), account.getAccType());
        if (existing != null) {
            return existing; // idempotent — if it somehow already exists, don't error, just return it
        }

        account.setStatus("ACTIVE");
        Account saved = repo.save(account);
        log("ACCOUNT_CREATED", saved.getAccId(), "Auto-created after KYC verification. Initial balance: " + saved.getBalance());
        return saved;
    }
}