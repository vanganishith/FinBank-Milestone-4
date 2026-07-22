package com.infosys.account_service.kafka;

import com.infosys.account_service.entity.Account;
import com.infosys.account_service.entity.AuditLog;
import com.infosys.account_service.event.LoanApprovedEvent;
import com.infosys.account_service.repository.AccountRepo;
import com.infosys.account_service.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LoanEventListener {

    @Autowired AccountRepo accountRepo;
    @Autowired AuditLogRepo auditRepo;

    @KafkaListener(topics = "loan-events", groupId = "finbank-group")
    public void handleLoanApproved(LoanApprovedEvent event) {
        Account account = accountRepo.findById(event.getAccId()).orElse(null);
        if (account == null) return;

        account.setBalance(account.getBalance() + event.getPrincipal());
        accountRepo.save(account);

        auditRepo.save(new AuditLog(null, "LOAN_DISBURSED", event.getAccId(),
            "Loan #" + event.getLoanId() + " disbursed: " + event.getPrincipal() + " via Kafka", LocalDateTime.now()));

        System.out.println("Loan disbursement credited: " + event);
    }
}