package com.infosys.payment_service.service;

import com.infosys.payment_service.entity.Account;
import com.infosys.payment_service.entity.Payment;
import com.infosys.payment_service.exception.FraudBlockedException;
import com.infosys.payment_service.exception.SagaExecutionException;
import com.infosys.payment_service.feign.AccountFeignClient;
import com.infosys.payment_service.repository.PaymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    PaymentRepo paymentRepo;
    @Autowired
    AccountFeignClient accountFeignClient;
    @Autowired
    FraudCheckService fraudCheckService;
    @Autowired
    PaymentSaga saga;

    public Payment initiateTransfer(Integer fromAccId, Integer toAccId, Double amount, String method,
            Integer beneficiaryId) {
        Account fromAccount = accountFeignClient.getAccount(fromAccId);
        if (fromAccount == null || !"ACTIVE".equals(fromAccount.getStatus())) {
            throw new RuntimeException("Sender account is not active or does not exist");
        }
        if (fromAccount.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }

        Account toAccount = accountFeignClient.getAccount(toAccId);
        if (toAccount == null || !"ACTIVE".equals(toAccount.getStatus())) {
            throw new RuntimeException("Receiver account is not active or does not exist");
        }

        int fraudScore = fraudCheckService.assess(fromAccId, toAccId, amount);

        Payment payment = new Payment();
        payment.setFromAccId(fromAccId);
        payment.setToAccId(toAccId);
        payment.setBeneficiaryId(beneficiaryId);
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setFraudScore(fraudScore);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setStatus("PENDING");

        if (fraudCheckService.isBlocked(fraudScore)) {
            payment.setStatus("FRAUD_BLOCKED");
            payment.setFailureReason("Fraud score " + fraudScore + " exceeded threshold");
            return paymentRepo.save(payment);
        }

        Payment saved = paymentRepo.save(payment);

        try {
            return saga.execute(saved);
        } catch (SagaExecutionException e) {
            return saved; // saga already recorded failure state
        }
    }

    public Account getFromAccountForAuthCheck(Integer accId) {
        return accountFeignClient.getAccount(accId);
    }
}