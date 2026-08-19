package com.infosys.kyc_service.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.infosys.kyc_service.entity.KycApplication;

@Repository
public interface KycApplicationRepo extends CrudRepository<KycApplication, Integer> {
    List<KycApplication> findByCustId(Integer custId);
    List<KycApplication> findByStatus(String status);
}