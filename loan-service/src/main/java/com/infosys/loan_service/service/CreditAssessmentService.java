package com.infosys.loan_service.service;

import org.springframework.stereotype.Service;

@Service
public class CreditAssessmentService {

    public double calculateEmi(double principal, double annualRatePercent, int tenureMonths) {
        double monthlyRate = (annualRatePercent / 12) / 100;
        if (monthlyRate == 0) return principal / tenureMonths;
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * factor) / (factor - 1);
    }
}