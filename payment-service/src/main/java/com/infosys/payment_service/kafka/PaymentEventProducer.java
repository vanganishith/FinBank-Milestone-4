package com.infosys.payment_service.kafka;

import com.infosys.payment_service.event.PaymentEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PaymentEvent event) {
        kafkaTemplate.send("payment-events", event);
    }
}