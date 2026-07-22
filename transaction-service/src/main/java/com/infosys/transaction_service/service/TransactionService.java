package com.infosys.transaction_service.service;

import com.infosys.transaction_service.entity.Account;
import com.infosys.transaction_service.entity.Transaction;
import com.infosys.transaction_service.event.TransactionEvent;
import com.infosys.transaction_service.exception.InsufficientBalanceException;
import com.infosys.transaction_service.exception.AccountNotActiveException;
import com.infosys.transaction_service.feign.AccountFeignClient;
import com.infosys.transaction_service.kafka.TransactionEventProducer;
import com.infosys.transaction_service.repository.TransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionService {

    @Autowired
    TransactionRepo repo;

    @Autowired
    AccountFeignClient feignClient;

    @Autowired
    TransactionEventProducer eventProducer;

    public Transaction deposit(Integer accId, Double amount) {
        Account account = feignClient.getAccount(accId);

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new AccountNotActiveException(
                    "Account " + accId + " is not active. Current status: " + account.getStatus());
        }

        account.setBalance(account.getBalance() + amount);
        feignClient.updateAccount(account);

        Transaction txn = new Transaction(null, accId, "DEPOSIT", amount, LocalDateTime.now());
        Transaction saved = repo.save(txn);
        eventProducer.publish(new TransactionEvent(accId, "DEPOSIT", amount, LocalDateTime.now().toString()));
        return saved;
    }

    public Transaction withdraw(Integer accId, Double amount) {
        Account account = feignClient.getAccount(accId);

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new AccountNotActiveException(
                    "Account " + accId + " is not active. Current status: " + account.getStatus());
        }

        if (account.getBalance() < amount) {
            throw new InsufficientBalanceException("Insufficient balance for account " + accId);
        }

        account.setBalance(account.getBalance() - amount);
        feignClient.updateAccount(account);

        Transaction txn = new Transaction(null, accId, "WITHDRAW", amount, LocalDateTime.now());
        Transaction saved = repo.save(txn);
        eventProducer.publish(new TransactionEvent(accId, "WITHDRAW", amount, LocalDateTime.now().toString()));
        return saved;
    }
}