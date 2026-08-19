package com.infosys.kyc_service.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.infosys.kyc_service.entity.KycAuditLog;

@Repository
public interface KycAuditLogRepo extends CrudRepository<KycAuditLog, Integer> {
    List<KycAuditLog> findByApplicationIdOrderByTimestampAsc(Integer applicationId);
}