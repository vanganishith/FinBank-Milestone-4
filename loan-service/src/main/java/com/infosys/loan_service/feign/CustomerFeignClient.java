package com.infosys.loan_service.feign;

import com.infosys.loan_service.entity.Customer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {
    @GetMapping("/customer/internal/{custId}")
    Customer getCustomer(@PathVariable Integer custId);
}