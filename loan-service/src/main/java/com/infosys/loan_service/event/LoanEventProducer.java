package com.infosys.loan_service.kafka;

import com.infosys.loan_service.event.LoanApprovedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LoanEventProducer {

    @Autowired
    private KafkaTemplate<String, LoanApprovedEvent> kafkaTemplate;

    public void publishLoanApproved(LoanApprovedEvent event) {
        kafkaTemplate.send("loan-events", event);
    }
}