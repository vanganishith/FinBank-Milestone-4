package com.infosys.account_service.controller;

import com.infosys.account_service.dto.AccountApplicationRequest;
import com.infosys.account_service.entity.AccountApplication;
import com.infosys.account_service.service.AccountApplicationService;
import com.infosys.account_service.util.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/account/application")
public class AccountApplicationController {

    @Autowired
    AccountApplicationService applicationService;

    @Autowired
    AuthUtil authUtil;

    @PostMapping("/apply")
    public AccountApplication apply(@RequestBody AccountApplicationRequest req,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, req.getCustId());
        return applicationService.apply(req.getCustId(), req.getRequestedAccType(),
                req.getInitialDeposit(), req.getDocumentsSubmitted());
    }

    @GetMapping("/pending-review")
    public List<AccountApplication> pendingReview(@RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return applicationService.getPendingReview();
    }

    @PutMapping("/{applicationId}/approve")
    public AccountApplication approve(@PathVariable Integer applicationId,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return applicationService.approve(applicationId);
    }

    @PutMapping("/{applicationId}/reject")
    public AccountApplication reject(@PathVariable Integer applicationId,
            @RequestParam(required = false) String reason,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireTeller(authHeader);
        return applicationService.reject(applicationId, reason);
    }

    @GetMapping("/customer/{custId}")
    public List<AccountApplication> byCustomer(@PathVariable Integer custId,
            @RequestHeader("Authorization") String authHeader) {
        authUtil.requireOwnershipOrTeller(authHeader, custId);
        return applicationService.getByCustomer(custId);
    }
}