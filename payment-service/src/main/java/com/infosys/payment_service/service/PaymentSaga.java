package com.infosys.payment_service.service;

import com.infosys.payment_service.entity.*;
import com.infosys.payment_service.event.PaymentEvent;
import com.infosys.payment_service.exception.SagaExecutionException;
import com.infosys.payment_service.feign.AccountFeignClient;
import com.infosys.payment_service.kafka.PaymentEventProducer;
import com.infosys.payment_service.repository.PaymentRepo;
import com.infosys.payment_service.repository.SagaLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.infosys.payment_service.service.SettlementService;
import java.time.LocalDateTime;

@Service
public class PaymentSaga {

    @Autowired
    AccountFeignClient accountFeignClient;

    @Autowired
    PaymentRepo paymentRepo;

    @Autowired
    SagaLogRepo sagaLogRepo;

    @Autowired
    PaymentEventProducer eventProducer;

    @Autowired
    SettlementService settlementService;

    private void log(Integer paymentId, String step, String status, String detail) {
        sagaLogRepo.save(new SagaLog(null, paymentId, step, status, detail, LocalDateTime.now()));
    }

    /**
     * Orchestrates: Debit sender -> Credit receiver -> Publish event. Compensates
     * debit if credit fails.
     */
    public Payment execute(Payment payment) {
        Integer paymentId = payment.getPaymentId();

        Account fromAccount = accountFeignClient.getAccount(payment.getFromAccId());
        Account toAccount = accountFeignClient.getAccount(payment.getToAccId());

        // ---- Step 1: Debit sender ----
        log(paymentId, "DEBIT_SENDER", "STARTED",
                "Debiting " + payment.getAmount() + " from account " + payment.getFromAccId());
        Double senderOriginalBalance = fromAccount.getBalance();
        try {
            fromAccount.setBalance(senderOriginalBalance - payment.getAmount());
            accountFeignClient.updateAccount(fromAccount);
            log(paymentId, "DEBIT_SENDER", "SUCCESS", "New sender balance: " + fromAccount.getBalance());
        } catch (Exception e) {
            log(paymentId, "DEBIT_SENDER", "FAILED", e.getMessage());
            markFailed(payment, "Failed to debit sender: " + e.getMessage());
            throw new SagaExecutionException("Saga failed at DEBIT_SENDER", e);
        }

        // ---- Step 2: Credit receiver ----
        log(paymentId, "CREDIT_RECEIVER", "STARTED",
                "Crediting " + payment.getAmount() + " to account " + payment.getToAccId());
        try {
            toAccount.setBalance(toAccount.getBalance() + payment.getAmount());
            accountFeignClient.updateAccount(toAccount);
            log(paymentId, "CREDIT_RECEIVER", "SUCCESS", "New receiver balance: " + toAccount.getBalance());
        } catch (Exception e) {
            log(paymentId, "CREDIT_RECEIVER", "FAILED", e.getMessage());
            compensateDebit(paymentId, fromAccount, senderOriginalBalance);
            markFailed(payment, "Failed to credit receiver: " + e.getMessage());
            throw new SagaExecutionException("Saga failed at CREDIT_RECEIVER", e);
        }

        // ---- Step 3: Settlement ----
        log(paymentId, "SETTLEMENT", "STARTED", "Settling payment, generating UTR");
        String utr = settlementService.generateUtr(payment.getMethod());
        payment.setUtr(utr);
        payment.setSettlementStatus("SETTLED");
        payment.setStatus("SUCCESS");
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepo.save(payment);
        log(paymentId, "SETTLEMENT", "SUCCESS", "UTR: " + utr + " | Settlement: T+0 instant");

        log(paymentId, "PUBLISH_EVENT", "STARTED", "Publishing PaymentEvent");
        try {
            eventProducer.publish(new PaymentEvent(paymentId, payment.getFromAccId(), payment.getToAccId(),
                    payment.getAmount(), LocalDateTime.now().toString()));
            log(paymentId, "PUBLISH_EVENT", "SUCCESS", "Event published");
        } catch (Exception e) {
            log(paymentId, "PUBLISH_EVENT", "FAILED", e.getMessage());
        }

        return payment;
    }

    private void compensateDebit(Integer paymentId, Account fromAccount, Double originalBalance) {
        log(paymentId, "COMPENSATE_DEBIT", "STARTED", "Reversing debit, restoring balance to " + originalBalance);
        try {
            fromAccount.setBalance(originalBalance);
            accountFeignClient.updateAccount(fromAccount);
            log(paymentId, "COMPENSATE_DEBIT", "SUCCESS", "Sender balance restored to " + originalBalance);
        } catch (Exception e) {
            log(paymentId, "COMPENSATE_DEBIT", "FAILED", "MANUAL RECONCILIATION NEEDED: " + e.getMessage());
        }
    }

    private void markFailed(Payment payment, String reason) {
        payment.setStatus("FAILED");
        payment.setFailureReason(reason);
        paymentRepo.save(payment);
    }
}