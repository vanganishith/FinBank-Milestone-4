package com.infosys.payment_service.repository;

import com.infosys.payment_service.entity.Payment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepo extends CrudRepository<Payment, Integer> {
    List<Payment> findByFromAccIdOrToAccId(Integer fromAccId, Integer toAccId);
}