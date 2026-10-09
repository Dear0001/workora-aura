package com.workora.dashboard.security;

import com.workora.common.security.KeycloakRoleAuthoritiesConverter;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleAuthoritiesConverterTest {
    private final KeycloakRoleAuthoritiesConverter converter = new KeycloakRoleAuthoritiesConverter();

    @Test
    void readsKeycloakRealmAccessRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void acceptsExistingFlatRoleClaimWithoutDuplicatingRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("ADMIN"))
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
