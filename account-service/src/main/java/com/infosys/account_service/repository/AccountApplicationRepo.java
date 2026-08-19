package com.infosys.account_service.repository;

import com.infosys.account_service.entity.AccountApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountApplicationRepo extends CrudRepository<AccountApplication, Integer> {
    List<AccountApplication> findByCustId(Integer custId);
    List<AccountApplication> findByStatus(String status);
}