package com.workora.dashboard.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSystemHealthServiceTest {
    @Test
    void reportsServiceReadinessAndDatabaseComponentStatus() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/actuator/health", exchange -> {
            byte[] response = """
                    {"status":"UP","components":{"db":{"status":"UP"}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
            AdminSystemHealthService service = new AdminSystemHealthService(
                    endpoint, endpoint, endpoint, endpoint, endpoint,
                    endpoint, endpoint, endpoint, endpoint);

            var statuses = service.checkServices();

            assertThat(statuses).hasSize(9).allSatisfy(status -> {
                assertThat(status.status()).isEqualTo("UP");
                assertThat(status.components()).containsEntry("db", "UP");
            });
        } finally {
            server.stop(0);
        }
    }
}
