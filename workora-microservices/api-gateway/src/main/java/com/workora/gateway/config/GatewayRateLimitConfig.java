package com.workora.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    public KeyResolver remoteAddressKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest().getRemoteAddress() == null
                        ? "anonymous"
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
