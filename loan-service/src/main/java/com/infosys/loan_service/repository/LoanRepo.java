package com.infosys.loan_service.repository;

import com.infosys.loan_service.entity.Loan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanRepo extends CrudRepository<Loan, Integer> {
    List<Loan> findByCustId(Integer custId);
}