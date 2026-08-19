package com.infosys.account_service.kafka;

import com.infosys.account_service.entity.Notification;
import com.infosys.account_service.event.PaymentEvent;
import com.infosys.account_service.repository.NotificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentEventListener {

    @Autowired
    NotificationRepo notificationRepo;

    @KafkaListener(topics = "payment-events", groupId = "finbank-group", containerFactory = "paymentKafkaListenerContainerFactory")
    public void handlePaymentEvent(PaymentEvent event) {
        System.out.println("Received Payment Kafka event: " + event);

        notificationRepo.save(new Notification(null, event.getFromAccId(), "PAYMENT_SENT",
                "You sent ₹" + event.getAmount() + " to account " + event.getToAccId(),
                false, LocalDateTime.now()));

        notificationRepo.save(new Notification(null, event.getToAccId(), "PAYMENT_RECEIVED",
                "You received ₹" + event.getAmount() + " from account " + event.getFromAccId(),
                false, LocalDateTime.now()));
    }
}