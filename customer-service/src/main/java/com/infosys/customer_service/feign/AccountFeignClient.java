package com.infosys.customer_service.feign;

import com.infosys.customer_service.entity.AccountCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountFeignClient {

    @PostMapping("/account/internal/create")
    Object createAccount(@RequestBody AccountCreateRequest request);
}