package com.workora.payment.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "workora.events";
    @Bean
    TopicExchange workoraEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
