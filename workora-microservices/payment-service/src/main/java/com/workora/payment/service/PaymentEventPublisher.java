package com.workora.payment.service;

import com.workora.payment.entity.Payment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class PaymentEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }
    public void paid(Payment payment) {
        rabbitTemplate.convertAndSend("workora.events", "payment.paid", Map.of(
                "eventType", "payment.paid",
                "userId", payment.getPayerId().toString(),
                "paymentId", payment.getId().toString(),
                "reference", payment.getReference(),
                "amount", payment.getAmount().toPlainString(),
                "currency", payment.getCurrency()));
    }
}
