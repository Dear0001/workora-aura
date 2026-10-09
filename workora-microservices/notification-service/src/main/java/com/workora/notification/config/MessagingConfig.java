package com.workora.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    @Bean
    TopicExchange workoraEventsExchange() { return new TopicExchange("workora.events", true, false); }
    @Bean
    Queue notificationEventsQueue() { return QueueBuilder.durable("notification.events").build(); }
    @Bean
    Binding paymentBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("payment.paid");
    }

    @Bean
    Binding taskBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("task.#");
    }

    @Bean
    Binding bugBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("bug-report.#");
    }

    @Bean
    Binding recruitmentBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("recruitment.#");
    }

    @Bean
    Binding projectBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("project.#");
    }

    @Bean
    Binding identityBinding(Queue notificationEventsQueue, TopicExchange workoraEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(workoraEventsExchange).with("identity.#");
    }
}
