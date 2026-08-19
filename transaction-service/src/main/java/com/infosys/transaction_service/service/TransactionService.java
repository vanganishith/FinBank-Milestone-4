package com.infosys.transaction_service.service;

import com.infosys.transaction_service.entity.Account;
import com.infosys.transaction_service.entity.Transaction;
import com.infosys.transaction_service.exception.InsufficientBalanceException;
import com.infosys.transaction_service.exception.AccountNotActiveException;
import com.infosys.transaction_service.exception.SagaExecutionException;
import com.infosys.transaction_service.feign.AccountFeignClient;
import com.infosys.transaction_service.dto.StatementLine;
import com.infosys.transaction_service.dto.StatementResponse;
import com.infosys.transaction_service.repository.TransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    TransactionRepo repo;

    @Autowired
    AccountFeignClient feignClient;

    @Autowired
    TransactionSaga saga;

    public Transaction deposit(Integer accId, Double amount) {
        Account account = feignClient.getAccount(accId);

        if (!"ACTIVE".equals(account.getStatus())) {
            throw new AccountNotActiveException(
                    "Account " + accId + " is not active. Current status: " + account.getStatus());
        }

        Double originalBalance = account.getBalance();
        account.setBalance(originalBalance + amount);

        try {
            return saga.execute(accId, "DEPOSIT", amount, account, originalBalance);
        } catch (SagaExecutionException e) {
            throw new RuntimeException("Deposit failed and was rolled back: " + e.getMessage(), e);
        }
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

        Double originalBalance = account.getBalance();
        account.setBalance(originalBalance - amount);

        try {
            return saga.execute(accId, "WITHDRAW", amount, account, originalBalance);
        } catch (SagaExecutionException e) {
            throw new RuntimeException("Withdraw failed and was rolled back: " + e.getMessage(), e);
        }
    }

    public StatementResponse getStatement(Integer accId, LocalDateTime from, LocalDateTime to) {
        Account account = feignClient.getAccount(accId);
        Double currentBalance = account.getBalance();

        List<Transaction> all = repo.findByAccId(accId);
        all.sort(Comparator.comparing(Transaction::getTxnDate));

        double runningBalance = currentBalance;
        List<StatementLine> reversedLines = new ArrayList<>();

        for (int i = all.size() - 1; i >= 0; i--) {
            Transaction t = all.get(i);
            if (!t.getTxnDate().isBefore(from) && !t.getTxnDate().isAfter(to)) {
                reversedLines.add(new StatementLine(t.getTxnId(), t.getType(), t.getAmount(), t.getTxnDate(), runningBalance));
            }
            if ("DEPOSIT".equals(t.getType())) {
                runningBalance -= t.getAmount();
            } else if ("WITHDRAW".equals(t.getType())) {
                runningBalance += t.getAmount();
            }
        }

        List<StatementLine> lines = new ArrayList<>(reversedLines);
        java.util.Collections.reverse(lines);

        Double openingBalance = lines.isEmpty() ? currentBalance
                : (lines.get(0).getType().equals("DEPOSIT")
                        ? lines.get(0).getRunningBalance() - lines.get(0).getAmount()
                        : lines.get(0).getRunningBalance() + lines.get(0).getAmount());

        Double closingBalance = lines.isEmpty() ? currentBalance : lines.get(lines.size() - 1).getRunningBalance();

        return new StatementResponse(accId, from, to, openingBalance, closingBalance, lines);
    }
}