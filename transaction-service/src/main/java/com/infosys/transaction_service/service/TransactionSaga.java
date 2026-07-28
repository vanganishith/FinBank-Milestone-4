package com.infosys.transaction_service.service;

import com.infosys.transaction_service.entity.Account;
import com.infosys.transaction_service.entity.SagaLog;
import com.infosys.transaction_service.entity.Transaction;
import com.infosys.transaction_service.event.TransactionEvent;
import com.infosys.transaction_service.exception.SagaExecutionException;
import com.infosys.transaction_service.feign.AccountFeignClient;
import com.infosys.transaction_service.kafka.TransactionEventProducer;
import com.infosys.transaction_service.repository.SagaLogRepo;
import com.infosys.transaction_service.repository.TransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionSaga {

    @Autowired
    AccountFeignClient feignClient;
    @Autowired
    TransactionRepo repo;
    @Autowired
    SagaLogRepo sagaLogRepo;
    @Autowired
    TransactionEventProducer eventProducer;

    private void log(Integer accId, String step, String status, String detail) {
        sagaLogRepo.save(new SagaLog(null, accId, step, status, detail, LocalDateTime.now()));
    }

    /**
     * Orchestrates: Account (balance update) -> Transaction (save record) -> Event (publish).
     * If saving the transaction record fails, the balance change is compensated (reversed).
     */
    public Transaction execute(Integer accId, String type, Double amount, Account account, Double originalBalance) {

        // ---- Step 1: Update account balance (already computed by caller) ----
        log(accId, "UPDATE_BALANCE", "STARTED", type + " " + amount + " on account " + accId);
        try {
            feignClient.updateAccount(account);
            log(accId, "UPDATE_BALANCE", "SUCCESS", "New balance: " + account.getBalance());
        } catch (Exception e) {
            log(accId, "UPDATE_BALANCE", "FAILED", e.getMessage());
            throw new SagaExecutionException("Saga failed at UPDATE_BALANCE", e);
        }

        // ---- Step 2: Save transaction record ----
        log(accId, "SAVE_TRANSACTION", "STARTED", "Persisting " + type + " record");
        Transaction saved;
        try {
            Transaction txn = new Transaction(null, accId, type, amount, LocalDateTime.now());
            saved = repo.save(txn);
            log(accId, "SAVE_TRANSACTION", "SUCCESS", "Transaction id: " + saved.getTxnId());
        } catch (Exception e) {
            log(accId, "SAVE_TRANSACTION", "FAILED", e.getMessage());
            compensateBalance(accId, account, originalBalance);
            throw new SagaExecutionException("Saga failed at SAVE_TRANSACTION", e);
        }

        // ---- Step 3: Publish event ----
        log(accId, "PUBLISH_EVENT", "STARTED", "Publishing TransactionEvent");
        try {
            eventProducer.publish(new TransactionEvent(accId, type, amount, LocalDateTime.now().toString()));
            log(accId, "PUBLISH_EVENT", "SUCCESS", "Event published");
        } catch (Exception e) {
            // Non-critical to money movement; log but don't compensate.
            log(accId, "PUBLISH_EVENT", "FAILED", e.getMessage());
        }

        return saved;
    }

    private void compensateBalance(Integer accId, Account account, Double originalBalance) {
        log(accId, "COMPENSATE_BALANCE", "STARTED", "Reversing balance to " + originalBalance);
        try {
            account.setBalance(originalBalance);
            feignClient.updateAccount(account);
            log(accId, "COMPENSATE_BALANCE", "SUCCESS", "Balance restored to " + originalBalance);
        } catch (Exception e) {
            log(accId, "COMPENSATE_BALANCE", "FAILED", "MANUAL RECONCILIATION NEEDED: " + e.getMessage());
        }
    }
}