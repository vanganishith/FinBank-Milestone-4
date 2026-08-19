package com.infosys.kyc_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.infosys.kyc_service.entity.Customer;

@FeignClient(name = "customer-service")
public interface CustomerFeignClient {
    @GetMapping("/customer/internal/{custId}")
    Customer getCustomer(@PathVariable Integer custId);

    @PutMapping("/customer/internal/kyc/verify/{custId}")
    Customer verifyKyc(@PathVariable Integer custId);

    @PutMapping("/customer/internal/kyc/reject/{custId}")
    Customer rejectKyc(@PathVariable Integer custId, @RequestParam String reason);
}