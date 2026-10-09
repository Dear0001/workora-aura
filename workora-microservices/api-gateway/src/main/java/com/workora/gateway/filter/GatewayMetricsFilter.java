package com.workora.gateway.filter;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class GatewayMetricsFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayMetricsFilter.class);

    private final MeterRegistry meterRegistry;

    public GatewayMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        final String method = Objects.requireNonNullElse(exchange.getRequest().getMethod(), org.springframework.http.HttpMethod.GET).name();
        final String path = exchange.getRequest().getURI().getPath();
        final String routeId;
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            routeId = route.getId();
        } else {
            routeId = "unknown";
        }

        final long startNanos = System.nanoTime();

        return chain.filter(exchange)
                .doOnSuccess(unused -> record(exchange, routeId, method, path, startNanos, false))
                .doOnError(throwable -> {
                    log.debug("Gateway request failed for {} {}", method, path, throwable);
                    record(exchange, routeId, method, path, startNanos, true);
                });
    }

    private void record(ServerWebExchange exchange, String routeId, String method, String path, long startNanos, boolean failed) {
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int httpStatus = status == null ? 500 : status.value();
        String statusTag = Integer.toString(httpStatus);
        var duration = Duration.ofNanos(System.nanoTime() - startNanos);

        meterRegistry.counter("workora.gateway.requests",
                "route", routeId,
                "method", method,
                "status", statusTag,
                "path", path,
                "failed", Boolean.toString(failed)).increment();

        meterRegistry.timer("workora.gateway.request.duration",
                "route", routeId,
                "method", method,
                "status", statusTag,
                "path", path).record(duration);
    }
}
