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

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    TransactionService service;

    @Autowired
    TransactionRepo repo;

    @Autowired
    SagaLogRepo sagaLogRepo;

    @PostMapping("/deposit/{accId}/{amount}")
    public Transaction deposit(@PathVariable Integer accId, @PathVariable Double amount) {
        return service.deposit(accId, amount);
    }

    @PostMapping("/withdraw/{accId}/{amount}")
    public Transaction withdraw(@PathVariable Integer accId, @PathVariable Double amount) {
        return service.withdraw(accId, amount);
    }

    @GetMapping("/all")
    public List<Transaction> getAllTransactions() {
        return (List<Transaction>) repo.findAll();
    }

    @GetMapping("/account/{accId}")
    public List<Transaction> getTransactionsByAccount(@PathVariable Integer accId) {
        return repo.findByAccId(accId);
    }

    @GetMapping("/saga-log/{accId}")
    public List<SagaLog> sagaLog(@PathVariable Integer accId) {
        return sagaLogRepo.findByAccIdOrderBySagaLogIdAsc(accId);
    }

    @GetMapping("/statement/{accId}")
    public StatementResponse getStatement(
            @PathVariable Integer accId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDateTime fromDate = (from != null) ? LocalDateTime.parse(from) : LocalDateTime.now().minusYears(10);
        LocalDateTime toDate = (to != null) ? LocalDateTime.parse(to) : LocalDateTime.now();

        return service.getStatement(accId, fromDate, toDate);
    }
}