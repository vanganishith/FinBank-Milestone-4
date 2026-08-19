package com.infosys.account_service.repository;

import com.infosys.account_service.entity.Account;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountRepo extends CrudRepository<Account, Integer> {
    List<Account> findByCustId(Integer custId);
    Account findByCustIdAndAccType(Integer custId, String accType);
}