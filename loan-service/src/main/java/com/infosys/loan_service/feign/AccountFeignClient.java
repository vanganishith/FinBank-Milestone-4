package com.infosys.loan_service.feign;

import com.infosys.loan_service.entity.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "account-service")
public interface AccountFeignClient {
    @GetMapping("/account/{accId}")
    Account getAccount(@PathVariable Integer accId);

    @PutMapping("/account/update")
    Account updateAccount(@RequestBody Account account);
}