package com.infosys.loan_service.repository;

import com.infosys.loan_service.entity.Repayment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RepaymentRepo extends CrudRepository<Repayment, Integer> {
    List<Repayment> findByLoanId(Integer loanId);
    List<Repayment> findByStatus(String status);
}