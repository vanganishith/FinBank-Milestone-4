package com.infosys.account_service.service;

import com.infosys.account_service.entity.Account;
import com.infosys.account_service.feign.CustomerFeignClient;
import com.infosys.account_service.repository.AccountRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    @Autowired
    AccountRepo repo;

    @Autowired
    CustomerFeignClient feignClient;

    public Account getAccountWithCustomer(Integer accId) {
        Account account = repo.findById(accId).get();
        account.setCustomer(feignClient.getCustomer(account.getCustId()));
        return account;
    }
}