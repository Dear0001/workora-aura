package com.workora.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void actuatorHealthIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void swaggerUiIsPublic() {
        webTestClient.get()
                .uri("/gateway/swagger-ui.html")
                .exchange()
                .expectStatus().isFound();
    }

    @Test
    void protectedResourcesRequireAuthentication() {
        webTestClient.get()
                .uri("/api/identity/api/v1/users/me")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
