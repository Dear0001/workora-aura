package com.workora.organization;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.mockito.Mockito.mock;

/**
 * Supplies a mock {@link JwtDecoder} so the security filter chain can start
 * without contacting a real Keycloak issuer. Tests inject the caller identity
 * with the spring-security-test {@code jwt()} request post-processor, so the
 * decoder is never actually invoked.
 */
@TestConfiguration
public class TestJwtConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }
}
