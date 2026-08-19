package com.infosys.transaction_service.repository;

import com.infosys.transaction_service.entity.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepo extends CrudRepository<Transaction, Integer> {
    List<Transaction> findByAccId(Integer accId);
}