package com.infosys.payment_service.controller;

import com.infosys.payment_service.entity.*;
import com.infosys.payment_service.repository.*;
import com.infosys.payment_service.service.PaymentService;
import com.infosys.payment_service.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.infosys.payment_service.feign.AccountFeignClient;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    PaymentService paymentService;
    @Autowired
    PaymentRepo paymentRepo;
    @Autowired
    BeneficiaryRepo beneficiaryRepo;
    @Autowired
    SagaLogRepo sagaLogRepo;
    @Autowired
    AuthUtil authUtil;
    @Autowired
    AccountFeignClient accountFeignClient;

    @PostMapping("/beneficiary/add")
    public Beneficiary addBeneficiary(@RequestBody Beneficiary beneficiary,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, beneficiary.getCustId());

        Account targetAccount = accountFeignClient.getAccount(beneficiary.getAccountNumber());
        if (targetAccount == null) {
            throw new com.infosys.payment_service.exception.InvalidRequestException(
                    "No account found with ID " + beneficiary.getAccountNumber() + ".");
        }
        if (!"ACTIVE".equals(targetAccount.getStatus())) {
            throw new com.infosys.payment_service.exception.InvalidRequestException(
                    "Cannot add beneficiary — target account is " + targetAccount.getStatus() + ".");
        }

        beneficiary.setAddedAt(LocalDateTime.now());
        return beneficiaryRepo.save(beneficiary);
    }

    @GetMapping("/beneficiary/{custId}")
    public List<Beneficiary> getBeneficiaries(@PathVariable Integer custId,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, custId);
        return beneficiaryRepo.findByCustId(custId);
    }

    @PostMapping("/transfer")
    public Payment transfer(@RequestParam Integer fromAccId, @RequestParam Integer toAccId,
            @RequestParam Double amount, @RequestParam(defaultValue = "IMPS") String method,
            @RequestParam(required = false) Integer beneficiaryId,
            @RequestHeader("Authorization") String authHeader) {

        com.infosys.payment_service.entity.Account fromAccount =
                paymentService.getFromAccountForAuthCheck(fromAccId);
        authUtil.requireOwnershipOrTeller(authHeader, fromAccount.getCustId());

        return paymentService.initiateTransfer(fromAccId, toAccId, amount, method, beneficiaryId);
    }

    @GetMapping("/{paymentId}")
    public Payment getPayment(@PathVariable Integer paymentId) {
        return paymentRepo.findById(paymentId).orElse(null);
    }

    @GetMapping("/account/{accId}")
    public List<Payment> getPaymentsByAccount(@PathVariable Integer accId,
            @RequestHeader("Authorization") String authHeader) {
        com.infosys.payment_service.entity.Account account = paymentService.getFromAccountForAuthCheck(accId);
        authUtil.requireOwnershipOrTeller(authHeader, account.getCustId());
        return paymentRepo.findByFromAccIdOrToAccId(accId, accId);
    }

    @GetMapping("/{paymentId}/saga-log")
    public List<SagaLog> sagaLog(@PathVariable Integer paymentId) {
        return sagaLogRepo.findByPaymentIdOrderBySagaLogIdAsc(paymentId);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        List<Payment> all = (List<Payment>) paymentRepo.findAll();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalPayments", all.size());
        long success = all.stream().filter(p -> "SUCCESS".equals(p.getStatus())).count();
        stats.put("successfulPayments", success);
        stats.put("successRate", all.isEmpty() ? 100.0 : Math.round((success * 10000.0 / all.size())) / 100.0);
        stats.put("totalVolume", all.stream().filter(p -> "SUCCESS".equals(p.getStatus())).mapToDouble(Payment::getAmount).sum());
        return stats;
    }
}