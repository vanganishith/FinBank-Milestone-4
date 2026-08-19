package com.infosys.transaction_service.feign;

import com.infosys.transaction_service.entity.Account;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "account-service")
public interface AccountFeignClient {

    @GetMapping("/account/internal/{accId}")
    Account getAccount(@PathVariable("accId") Integer accId);

    @PutMapping("/account/update")
    Account updateAccount(@RequestBody Account account);
}