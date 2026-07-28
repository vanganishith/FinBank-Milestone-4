package com.infosys.transaction_service.repository;

import com.infosys.transaction_service.entity.SagaLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SagaLogRepo extends CrudRepository<SagaLog, Integer> {
    List<SagaLog> findByAccIdOrderBySagaLogIdAsc(Integer accId);
}