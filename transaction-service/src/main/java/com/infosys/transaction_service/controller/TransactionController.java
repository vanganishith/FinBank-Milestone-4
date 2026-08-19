package com.infosys.transaction_service.controller;

import com.infosys.transaction_service.entity.Transaction;
import com.infosys.transaction_service.repository.TransactionRepo;
import com.infosys.transaction_service.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.infosys.transaction_service.dto.StatementResponse;
import java.time.LocalDateTime;
import java.util.List;
import com.infosys.transaction_service.entity.SagaLog;
import com.infosys.transaction_service.repository.SagaLogRepo;
import com.infosys.transaction_service.util.AuthUtil;
import com.infosys.transaction_service.entity.Account;
import com.infosys.transaction_service.feign.AccountFeignClient;
import com.infosys.transaction_service.exception.InvalidRequestException;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    TransactionService service;

    @Autowired
    TransactionRepo repo;

    @Autowired
    SagaLogRepo sagaLogRepo;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    AccountFeignClient accountFeignClient;

    @PostMapping("/deposit/{accId}/{amount}")
    public Transaction deposit(@PathVariable Integer accId, @PathVariable Double amount,
            @RequestHeader("Authorization") String authHeader) {
        Account account = accountFeignClient.getAccount(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return service.deposit(accId, amount);
    }

    @PostMapping("/withdraw/{accId}/{amount}")
    public Transaction withdraw(@PathVariable Integer accId, @PathVariable Double amount,
            @RequestHeader("Authorization") String authHeader) {
        Account account = accountFeignClient.getAccount(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return service.withdraw(accId, amount);
    }

    @GetMapping("/all")
    public List<Transaction> getAllTransactions(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return (List<Transaction>) repo.findAll();
    }

    @GetMapping("/account/{accId}")
    public List<Transaction> getTransactionsByAccount(@PathVariable Integer accId,
            @RequestHeader("Authorization") String authHeader) {
        Account account = accountFeignClient.getAccount(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return repo.findByAccId(accId);
    }

    @GetMapping("/saga-log/{accId}")
    public List<SagaLog> sagaLog(@PathVariable Integer accId, @RequestHeader("Authorization") String authHeader) {
        Account account = accountFeignClient.getAccount(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return sagaLogRepo.findByAccIdOrderBySagaLogIdAsc(accId);
    }

    @GetMapping("/statement/{accId}")
    public StatementResponse getStatement(
            @PathVariable Integer accId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestHeader("Authorization") String authHeader) {

        Account account = accountFeignClient.getAccount(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());

        LocalDateTime fromDate;
        LocalDateTime toDate;
        try {
            fromDate = (from != null) ? LocalDateTime.parse(from) : LocalDateTime.now().minusYears(10);
            toDate = (to != null) ? LocalDateTime.parse(to) : LocalDateTime.now();
        } catch (DateTimeParseException e) {
            throw new InvalidRequestException(
                    "Invalid date format. Use ISO format like 2026-08-01T00:00:00 for 'from' and 'to'.");
        }

        if (fromDate.isAfter(toDate)) {
            throw new InvalidRequestException("'from' date must be before 'to' date.");
        }

        return service.getStatement(accId, fromDate, toDate);
    }
}