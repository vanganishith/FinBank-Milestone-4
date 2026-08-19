package com.infosys.loan_service.controller;

import com.infosys.loan_service.dto.CreditProfile;
import com.infosys.loan_service.entity.Loan;
import com.infosys.loan_service.entity.Repayment;
import com.infosys.loan_service.repository.LoanRepo;
import com.infosys.loan_service.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.infosys.loan_service.dto.CollectionItem;
import java.util.*;
import com.infosys.loan_service.entity.SagaLog;
import com.infosys.loan_service.repository.SagaLogRepo;
import com.infosys.loan_service.util.AuthUtil;

@RestController
@RequestMapping("/loan")
public class LoanController {

    @Autowired
    LoanService loanService;

    @Autowired
    LoanRepo loanRepo;

    @Autowired
    SagaLogRepo sagaLogRepo;

    @Autowired
    AuthUtil authUtil;

    // ---- Customer applies — no auto-decision, just creates a PENDING_REVIEW application ----
    @PostMapping("/apply")
    public Loan apply(@RequestParam Integer custId, @RequestParam Integer accId,
            @RequestParam Double principal, @RequestParam Double interestRate,
            @RequestParam Integer tenureMonths,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireSelfOrTeller(authHeader, custId);
        return loanService.applyForLoan(custId, accId, principal, interestRate, tenureMonths);
    }

    // ---- Manager review queue ----
    @GetMapping("/pending-review")
    public List<Loan> pendingReview(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return loanService.getPendingReview();
    }

    @PutMapping("/{loanId}/approve")
    public Loan approve(@PathVariable Integer loanId, @RequestParam Integer creditScore,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return loanService.approveLoan(loanId, creditScore);
    }

    @PutMapping("/{loanId}/reject")
    public Loan reject(@PathVariable Integer loanId, @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireManager(authHeader);
        return loanService.rejectLoan(loanId, reason);
    }

    @GetMapping("/{loanId}")
    public Loan getLoan(@PathVariable Integer loanId, @RequestHeader("Authorization") String authHeader) {
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        authUtil.requireOwnershipOrTeller(authHeader, loan.getCustId());
        return loan;
    }

    @GetMapping("/customer/{custId}")
    public List<Loan> getByCustomer(@PathVariable Integer custId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireSelfOrTeller(authHeader, custId);
        return loanService.getLoansByCustomer(custId);
    }

    @GetMapping("/{loanId}/schedule")
    public List<Repayment> schedule(@PathVariable Integer loanId, @RequestHeader("Authorization") String authHeader) {
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        authUtil.requireOwnershipOrTeller(authHeader, loan.getCustId());
        return loanService.getSchedule(loanId);
    }

    @PutMapping("/repayment/{repaymentId}/pay")
    public Repayment pay(@PathVariable Integer repaymentId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return loanService.payInstallment(repaymentId);
    }

    @PutMapping("/{loanId}/check-npa")
    public Loan checkNpa(@PathVariable Integer loanId, @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return loanService.checkAndClassifyNpa(loanId);
    }

    @GetMapping("/credit-profile")
    public CreditProfile creditProfile(@RequestParam Integer custId, @RequestParam Integer accId,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireSelfOrTeller(authHeader, custId);
        return loanService.getCreditProfile(custId, accId);
    }

    @GetMapping("/all")
    public List<Loan> getAll(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return (List<Loan>) loanRepo.findAll();
    }

    @GetMapping("/collections")
    public List<CollectionItem> collections(@RequestParam(defaultValue = "7") int upcomingWindowDays,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return loanService.getCollectionsWorklist(upcomingWindowDays);
    }

    @GetMapping("/{loanId}/saga-log")
    public List<SagaLog> sagaLog(@PathVariable Integer loanId, @RequestHeader("Authorization") String authHeader) {
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        authUtil.requireOwnershipOrTeller(authHeader, loan.getCustId());
        return sagaLogRepo.findByLoanIdOrderBySagaLogIdAsc(loanId);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        List<Loan> all = (List<Loan>) loanRepo.findAll();
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalLoans", all.size());
        stats.put("activeLoans", all.stream().filter(l -> "ACTIVE".equals(l.getStatus())).count());
        stats.put("npaLoans", all.stream().filter(l -> "NPA".equals(l.getStatus())).count());
        stats.put("totalDisbursed", all.stream().filter(l -> l.getDisbursedAt() != null).mapToDouble(Loan::getPrincipal).sum());
        return stats;
    }
}