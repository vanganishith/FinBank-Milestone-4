package com.infosys.auth_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {

    @GetMapping("/customer/internal/by-username/{username}")
    CustomerAuthInternal getByUsername(@PathVariable String username);
}