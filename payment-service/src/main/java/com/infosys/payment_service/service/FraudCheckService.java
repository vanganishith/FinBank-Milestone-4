package com.infosys.payment_service.service;

import com.infosys.payment_service.repository.PaymentRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FraudCheckService {

    @Autowired
    PaymentRepo paymentRepo;

    private static final double LARGE_AMOUNT_THRESHOLD = 100000.0;
    private static final int VELOCITY_WINDOW_MINUTES = 10;
    private static final int VELOCITY_MAX_COUNT = 5;
    private static final int BLOCK_SCORE = 70;

    /** Returns a risk score 0-100. Throws if score crosses the block threshold. */
    public int assess(Integer fromAccId, Integer toAccId, Double amount) {
        int score = 0;

        if (fromAccId.equals(toAccId)) {
            score += 100; // self-transfer is nonsensical / clearly wrong
        }

        if (amount > LARGE_AMOUNT_THRESHOLD) {
            score += 40;
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        List<com.infosys.payment_service.entity.Payment> recent = paymentRepo.findByFromAccIdOrToAccId(fromAccId, fromAccId);
        long recentCount = recent.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(windowStart))
                .count();
        if (recentCount >= VELOCITY_MAX_COUNT) {
            score += 30;
        }

        return score;
    }

    public boolean isBlocked(int score) {
        return score >= BLOCK_SCORE;
    }
}