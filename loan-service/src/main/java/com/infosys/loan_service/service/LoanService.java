package com.infosys.loan_service.service;

import com.infosys.loan_service.dto.CreditProfile;
import com.infosys.loan_service.entity.*;
import com.infosys.loan_service.event.LoanApprovedEvent;
import com.infosys.loan_service.feign.AccountFeignClient;
import com.infosys.loan_service.feign.CustomerFeignClient;
import com.infosys.loan_service.kafka.LoanEventProducer;
import com.infosys.loan_service.repository.LoanRepo;
import com.infosys.loan_service.repository.RepaymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.infosys.loan_service.dto.CollectionItem;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.infosys.loan_service.exception.SagaExecutionException;

@Service
public class LoanService {

    @Autowired
    LoanRepo loanRepo;
    @Autowired
    RepaymentRepo repaymentRepo;
    @Autowired
    CustomerFeignClient customerFeignClient;
    @Autowired
    CreditAssessmentService creditService;
    @Autowired
    LoanEventProducer eventProducer;
    @Autowired
    AccountFeignClient accountFeignClient;
    @Autowired
    LoanDisbursementSaga disbursementSaga;

    private static final int MIN_APPROVAL_SCORE = 650;

    public CreditProfile getCreditProfile(Integer custId, Integer accId) {
        Customer customer = customerFeignClient.getCustomer(custId);
        Account account = accountFeignClient.getAccount(accId);
        List<Loan> loans = loanRepo.findByCustId(custId);

        long activeCount = loans.stream().filter(l -> "ACTIVE".equals(l.getStatus())).count();
        long npaCount = loans.stream().filter(l -> "NPA".equals(l.getStatus())).count();

        String suggestion;
        if (customer == null || !"VERIFIED".equals(customer.getKycStatus())) {
            suggestion = "KYC not verified — cannot approve any loan yet.";
        } else if (npaCount > 0) {
            suggestion = "Customer has " + npaCount
                    + " NPA loan(s) — high risk, recommend rejecting or requiring collateral.";
        } else if (activeCount >= 2) {
            suggestion = "Customer already has " + activeCount + " active loans — assess repayment capacity carefully.";
        } else if (account != null && account.getBalance() != null && account.getBalance() > 50000) {
            suggestion = "Healthy account balance and no repayment issues — likely low risk.";
        } else {
            suggestion = "No red flags found — proceed with standard credit score entry.";
        }

        return new CreditProfile(
                custId,
                customer != null ? customer.getName() : "Unknown",
                customer != null ? customer.getKycStatus() : "N/A",
                account != null ? account.getBalance() : null,
                account != null ? account.getStatus() : "N/A",
                loans,
                activeCount,
                npaCount,
                suggestion);
    }

    public Loan applyForLoan(Integer custId, Integer accId, Double principal, Double interestRate, Integer tenureMonths,
            Integer creditScore) {
        Customer customer = customerFeignClient.getCustomer(custId);

        Loan loan = new Loan();
        loan.setCustId(custId);
        loan.setAccId(accId);
        loan.setPrincipal(principal);
        loan.setInterestRate(interestRate);
        loan.setTenureMonths(tenureMonths);
        loan.setCreditScore(creditScore);
        loan.setAppliedAt(LocalDateTime.now());

        if (customer == null) {
            loan.setStatus("REJECTED");
            loan.setRejectionReason("Customer not found");
            return loanRepo.save(loan);
        }

        if (!"VERIFIED".equals(customer.getKycStatus())) {
            loan.setStatus("REJECTED");
            loan.setRejectionReason("KYC not verified. Current status: " + customer.getKycStatus());
            return loanRepo.save(loan);
        }

        if (creditScore < MIN_APPROVAL_SCORE) {
            loan.setStatus("REJECTED");
            loan.setRejectionReason(
                    "Credit score " + creditScore + " below minimum required (" + MIN_APPROVAL_SCORE + ")");
            return loanRepo.save(loan);
        }

        double emi = creditService.calculateEmi(principal, interestRate, tenureMonths);
        loan.setEmi(Math.round(emi * 100.0) / 100.0);
        loan.setStatus("APPROVED");

        Loan saved = loanRepo.save(loan);

        try {
            saved = disbursementSaga.disburse(saved);
        } catch (SagaExecutionException e) {
            // saga already recorded the failure state on the loan; return as-is
            return saved;
        }

        generateRepaymentSchedule(saved);

        return saved;
    }

    private void generateRepaymentSchedule(Loan loan) {
        List<Repayment> schedule = new ArrayList<>();
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);
        for (int i = 1; i <= loan.getTenureMonths(); i++) {
            Repayment r = new Repayment();
            r.setLoanId(loan.getLoanId());
            r.setInstallmentNumber(i);
            r.setDueDate(firstDueDate.plusMonths(i - 1));
            r.setAmount(loan.getEmi());
            r.setStatus("PENDING");
            schedule.add(r);
        }
        repaymentRepo.saveAll(schedule);
    }

    public Repayment payInstallment(Integer repaymentId) {
        Repayment r = repaymentRepo.findById(repaymentId)
                .orElseThrow(() -> new RuntimeException("Repayment not found"));
        r.setPaidDate(LocalDate.now());
        r.setStatus("PAID");
        return repaymentRepo.save(r);
    }

    /**
     * NPA classification: any PENDING installment whose due date is more than
     * 90 days in the past marks the whole loan as NPA.
     */
    public Loan checkAndClassifyNpa(Integer loanId) {
        Loan loan = loanRepo.findById(loanId).orElseThrow(() -> new RuntimeException("Loan not found"));
        List<Repayment> repayments = repaymentRepo.findByLoanId(loanId);

        boolean overdue90 = repayments.stream().anyMatch(
                r -> "PENDING".equals(r.getStatus()) && r.getDueDate().isBefore(LocalDate.now().minusDays(90)));

        if (overdue90 && !"NPA".equals(loan.getStatus())) {
            loan.setStatus("NPA");
            loanRepo.save(loan);
        }
        return loan;
    }

    public List<CollectionItem> getCollectionsWorklist(int upcomingWindowDays) {
        List<Repayment> pending = repaymentRepo.findByStatus("PENDING");
        LocalDate today = LocalDate.now();

        List<CollectionItem> worklist = pending.stream()
                .filter(r -> {
                    long daysUntilDue = ChronoUnit.DAYS.between(today, r.getDueDate());
                    // include anything overdue (negative window) or due within the upcoming window
                    return daysUntilDue <= upcomingWindowDays;
                })
                .map(r -> {
                    long daysOverdue = ChronoUnit.DAYS.between(r.getDueDate(), today);
                    String bucket;
                    if (daysOverdue >= 90)
                        bucket = "SERIOUSLY_OVERDUE";
                    else if (daysOverdue > 0)
                        bucket = "OVERDUE";
                    else if (daysOverdue == 0)
                        bucket = "DUE_TODAY";
                    else
                        bucket = "UPCOMING";

                    Loan loan = loanRepo.findById(r.getLoanId()).orElse(null);
                    Integer custId = loan != null ? loan.getCustId() : null;

                    return new CollectionItem(
                            r.getRepaymentId(), r.getLoanId(), custId, r.getInstallmentNumber(),
                            r.getDueDate(), r.getAmount(), daysOverdue, bucket);
                })
                .sorted(Comparator.comparingLong(CollectionItem::getDaysOverdue).reversed())
                .collect(Collectors.toList());

        return worklist;
    }

    public List<Repayment> getSchedule(Integer loanId) {
        return repaymentRepo.findByLoanId(loanId);
    }

    public List<Loan> getLoansByCustomer(Integer custId) {
        return loanRepo.findByCustId(custId);
    }
}