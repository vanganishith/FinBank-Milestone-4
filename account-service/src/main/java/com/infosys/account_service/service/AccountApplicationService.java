package com.infosys.account_service.service;

import com.infosys.account_service.entity.Account;
import com.infosys.account_service.entity.AccountApplication;
import com.infosys.account_service.entity.Customer;
import com.infosys.account_service.exception.InvalidRequestException;
import com.infosys.account_service.feign.CustomerFeignClient;
import com.infosys.account_service.repository.AccountApplicationRepo;
import com.infosys.account_service.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountApplicationService {

    @Autowired
    AccountApplicationRepo applicationRepo;
    @Autowired
    AccountRepo accountRepo;
    @Autowired
    CustomerFeignClient customerFeignClient;

    public AccountApplication apply(Integer custId, String requestedAccType, Double initialDeposit, String documentsSubmitted) {
        if (!"SAVINGS".equals(requestedAccType) && !"CURRENT".equals(requestedAccType)) {
            throw new InvalidRequestException("Account type must be SAVINGS or CURRENT.");
        }

        Customer customer = customerFeignClient.getCustomer(custId);
        if (customer == null) {
            throw new InvalidRequestException("Customer not found.");
        }

        Account existing = accountRepo.findByCustIdAndAccType(custId, requestedAccType);
        if (existing != null) {
            throw new InvalidRequestException(
                    "You already have a " + requestedAccType + " account (accId " + existing.getAccId() + ").");
        }

        List<AccountApplication> pending = applicationRepo.findByCustId(custId);
        boolean alreadyPending = pending.stream().anyMatch(
                a -> "PENDING_REVIEW".equals(a.getStatus()) && requestedAccType.equals(a.getRequestedAccType()));
        if (alreadyPending) {
            throw new InvalidRequestException("You already have a pending " + requestedAccType + " application.");
        }

        AccountApplication app = new AccountApplication();
        app.setCustId(custId);
        app.setRequestedAccType(requestedAccType);
        app.setInitialDeposit(initialDeposit != null ? initialDeposit : 0.0);
        app.setDocumentsSubmitted(documentsSubmitted);
        app.setStatus("PENDING_REVIEW");
        app.setAppliedAt(LocalDateTime.now());

        return applicationRepo.save(app);
    }

    public AccountApplication approve(Integer applicationId) {
        AccountApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"PENDING_REVIEW".equals(app.getStatus())) {
            throw new InvalidRequestException("Application is not pending review. Current status: " + app.getStatus());
        }

        Customer customer = customerFeignClient.getCustomer(app.getCustId());
        if (customer == null || !"VERIFIED".equals(customer.getKycStatus())) {
            throw new InvalidRequestException(
                    "Cannot approve. Customer KYC status is "
                            + (customer != null ? customer.getKycStatus() : "UNKNOWN")
                            + " — verify KYC first before approving this application.");
        }

        Account existing = accountRepo.findByCustIdAndAccType(app.getCustId(), app.getRequestedAccType());
        if (existing != null) {
            throw new InvalidRequestException(
                    "Customer already has a " + app.getRequestedAccType() + " account (accId " + existing.getAccId() + ").");
        }

        Account account = new Account();
        account.setCustId(app.getCustId());
        account.setAccType(app.getRequestedAccType());
        account.setBalance(app.getInitialDeposit());
        account.setStatus("ACTIVE");
        Account savedAccount = accountRepo.save(account);

        app.setStatus("APPROVED");
        app.setCreatedAccId(savedAccount.getAccId());
        app.setReviewedAt(LocalDateTime.now());
        applicationRepo.save(app);

        return app;
    }

    public AccountApplication reject(Integer applicationId, String reason) {
        AccountApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!"PENDING_REVIEW".equals(app.getStatus())) {
            throw new InvalidRequestException("Application is not pending review. Current status: " + app.getStatus());
        }

        app.setStatus("REJECTED");
        app.setRejectionReason(reason != null ? reason : "Not specified");
        app.setReviewedAt(LocalDateTime.now());
        return applicationRepo.save(app);
    }

    public List<AccountApplication> getPendingReview() {
        return applicationRepo.findByStatus("PENDING_REVIEW");
    }

    public List<AccountApplication> getByCustomer(Integer custId) {
        return applicationRepo.findByCustId(custId);
    }
}