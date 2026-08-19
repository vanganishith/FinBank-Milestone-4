package com.infosys.payment_service.repository;

import com.infosys.payment_service.entity.Beneficiary;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BeneficiaryRepo extends CrudRepository<Beneficiary, Integer> {
    List<Beneficiary> findByCustId(Integer custId);
}