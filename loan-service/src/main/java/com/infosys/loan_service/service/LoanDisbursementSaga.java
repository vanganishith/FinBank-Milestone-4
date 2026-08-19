package com.infosys.loan_service.service;

import com.infosys.loan_service.entity.*;
import com.infosys.loan_service.event.LoanApprovedEvent;
import com.infosys.loan_service.exception.SagaExecutionException;
import com.infosys.loan_service.feign.AccountFeignClient;
import com.infosys.loan_service.kafka.LoanEventProducer;
import com.infosys.loan_service.repository.LoanRepo;
import com.infosys.loan_service.repository.SagaLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoanDisbursementSaga {

    @Autowired
    AccountFeignClient accountFeignClient;
    @Autowired
    LoanRepo loanRepo;
    @Autowired
    SagaLogRepo sagaLogRepo;
    @Autowired
    LoanEventProducer eventProducer;

    private void log(Integer loanId, String step, String status, String detail) {
        sagaLogRepo.save(new SagaLog(null, loanId, step, status, detail, LocalDateTime.now()));
    }

    /**
     * Orchestrates: Account (credit) -> Loan (activate) -> Event (publish).
     * Any failure after the credit step triggers compensation (debit back).
     */
    public Loan disburse(Loan loan) {
        Integer loanId = loan.getLoanId();
        Account account = accountFeignClient.getAccount(loan.getAccId());

        // ---- Step 1: Credit account ----
        log(loanId, "CREDIT_ACCOUNT", "STARTED", "Crediting " + loan.getPrincipal() + " to account " + loan.getAccId());
        Double originalBalance = account.getBalance();
        try {
            account.setBalance(originalBalance + loan.getPrincipal());
            accountFeignClient.updateAccount(account);
            log(loanId, "CREDIT_ACCOUNT", "SUCCESS", "New balance: " + account.getBalance());
        } catch (Exception e) {
            log(loanId, "CREDIT_ACCOUNT", "FAILED", e.getMessage());
            loan.setStatus("DISBURSEMENT_FAILED");
            loan.setRejectionReason("Failed to credit account: " + e.getMessage());
            loanRepo.save(loan);
            throw new SagaExecutionException("Saga failed at CREDIT_ACCOUNT", e);
        }

        // ---- Step 2: Activate loan ----
        log(loanId, "ACTIVATE_LOAN", "STARTED", "Marking loan ACTIVE");
        try {
            loan.setStatus("ACTIVE");
            loan.setDisbursedAt(LocalDateTime.now());
            loanRepo.save(loan);
            log(loanId, "ACTIVATE_LOAN", "SUCCESS", "Loan marked ACTIVE");
        } catch (Exception e) {
            log(loanId, "ACTIVATE_LOAN", "FAILED", e.getMessage());
            compensateCreditAccount(loanId, account, originalBalance, loan.getPrincipal());
            loan.setStatus("DISBURSEMENT_FAILED");
            loan.setRejectionReason("Failed to activate loan: " + e.getMessage());
            loanRepo.save(loan);
            throw new SagaExecutionException("Saga failed at ACTIVATE_LOAN", e);
        }

        // ---- Step 3: Publish event ----
        log(loanId, "PUBLISH_EVENT", "STARTED", "Publishing LoanApprovedEvent");
        try {
            eventProducer.publishLoanApproved(
                    new LoanApprovedEvent(loanId, loan.getAccId(), loan.getPrincipal(), LocalDateTime.now().toString()));
            log(loanId, "PUBLISH_EVENT", "SUCCESS", "Event published");
        } catch (Exception e) {
            // Event publish failure is non-critical to money movement, but we still
            // record it — no compensation needed since balance + loan state are already correct.
            log(loanId, "PUBLISH_EVENT", "FAILED", e.getMessage());
        }

        return loan;
    }

    private void compensateCreditAccount(Integer loanId, Account account, Double originalBalance, Double principal) {
        log(loanId, "COMPENSATE_CREDIT_ACCOUNT", "STARTED", "Reversing credit of " + principal);
        try {
            account.setBalance(originalBalance);
            accountFeignClient.updateAccount(account);
            log(loanId, "COMPENSATE_CREDIT_ACCOUNT", "SUCCESS", "Balance restored to " + originalBalance);
        } catch (Exception e) {
            // If even the compensation fails, this needs manual reconciliation —
            // logged clearly so it's never silently lost.
            log(loanId, "COMPENSATE_CREDIT_ACCOUNT", "FAILED",
                    "MANUAL RECONCILIATION NEEDED: " + e.getMessage());
        }
    }
}