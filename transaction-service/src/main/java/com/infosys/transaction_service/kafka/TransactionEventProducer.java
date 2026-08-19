package com.infosys.transaction_service.kafka;

import com.infosys.transaction_service.event.TransactionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventProducer {

    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void publish(TransactionEvent event) {
        kafkaTemplate.send("transaction-events", event);
    }
}