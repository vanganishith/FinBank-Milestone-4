package com.infosys.loan_service.controller;

import com.infosys.loan_service.dto.CreditProfile;
import com.infosys.loan_service.entity.Loan;
import com.infosys.loan_service.entity.Repayment;
import com.infosys.loan_service.repository.LoanRepo;
import com.infosys.loan_service.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.infosys.loan_service.dto.CollectionItem;
import java.util.List;
import com.infosys.loan_service.entity.SagaLog;
import com.infosys.loan_service.repository.SagaLogRepo;

@RestController
@RequestMapping("/loan")
public class LoanController {

    @Autowired
    LoanService loanService;
    @Autowired
    LoanRepo loanRepo;
    @Autowired
    SagaLogRepo sagaLogRepo;

    @PostMapping("/apply")
    public Loan apply(@RequestParam Integer custId, @RequestParam Integer accId,
            @RequestParam Double principal, @RequestParam Double interestRate,
            @RequestParam Integer tenureMonths, @RequestParam Integer creditScore) {
        return loanService.applyForLoan(custId, accId, principal, interestRate, tenureMonths, creditScore);
    }

    @GetMapping("/{loanId}")
    public Loan getLoan(@PathVariable Integer loanId) {
        return loanRepo.findById(loanId).orElse(null);
    }

    @GetMapping("/customer/{custId}")
    public List<Loan> getByCustomer(@PathVariable Integer custId) {
        return loanService.getLoansByCustomer(custId);
    }

    @GetMapping("/{loanId}/schedule")
    public List<Repayment> schedule(@PathVariable Integer loanId) {
        return loanService.getSchedule(loanId);
    }

    @PutMapping("/repayment/{repaymentId}/pay")
    public Repayment pay(@PathVariable Integer repaymentId) {
        return loanService.payInstallment(repaymentId);
    }

    @PutMapping("/{loanId}/check-npa")
    public Loan checkNpa(@PathVariable Integer loanId) {
        return loanService.checkAndClassifyNpa(loanId);
    }

    @GetMapping("/credit-profile")
    public CreditProfile creditProfile(@RequestParam Integer custId, @RequestParam Integer accId) {
        return loanService.getCreditProfile(custId, accId);
    }

    @GetMapping("/all")
    public List<Loan> getAll() {
        return (List<Loan>) loanRepo.findAll();
    }

    @GetMapping("/collections")
    public List<CollectionItem> collections(@RequestParam(defaultValue = "7") int upcomingWindowDays) {
        return loanService.getCollectionsWorklist(upcomingWindowDays);
    }

    @GetMapping("/{loanId}/saga-log")
    public List<SagaLog> sagaLog(@PathVariable Integer loanId) {
        return sagaLogRepo.findByLoanIdOrderBySagaLogIdAsc(loanId);
    }
}