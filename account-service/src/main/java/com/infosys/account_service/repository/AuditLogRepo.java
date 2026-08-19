package com.infosys.account_service.repository;

import com.infosys.account_service.entity.AuditLog;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepo extends CrudRepository<AuditLog, Integer> {
    List<AuditLog> findByAccId(Integer accId);
}