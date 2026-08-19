package com.infosys.account_service.kafka;

import com.infosys.account_service.entity.AuditLog;
import com.infosys.account_service.event.TransactionEvent;
import com.infosys.account_service.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionEventListener {

    @Autowired
    AuditLogRepo auditRepo;

    @KafkaListener(topics = "transaction-events", groupId = "finbank-group")
    public void handleTransactionEvent(TransactionEvent event) {
        System.out.println("Received Kafka event: " + event);
        auditRepo.save(new AuditLog(null, "KAFKA_" + event.getType(), event.getAccId(),
            "Amount: " + event.getAmount() + " via Kafka event", LocalDateTime.now()));
    }
}