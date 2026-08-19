package com.infosys.payment_service.repository;

import com.infosys.payment_service.entity.SagaLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SagaLogRepo extends CrudRepository<SagaLog, Integer> {
    List<SagaLog> findByPaymentIdOrderBySagaLogIdAsc(Integer paymentId);
}