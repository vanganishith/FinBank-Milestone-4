package com.infosys.account_service.feign;

import com.infosys.account_service.entity.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {

    @GetMapping("/customer/internal/{custId}")
    Customer getCustomer(@PathVariable("custId") Integer custId);
}