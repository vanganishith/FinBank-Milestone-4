package com.infosys.account_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "auth-service")
public interface AuthFeignClient {

    @GetMapping("/auth/validate")
    Map<String, Object> validate(@RequestParam("token") String token);
}