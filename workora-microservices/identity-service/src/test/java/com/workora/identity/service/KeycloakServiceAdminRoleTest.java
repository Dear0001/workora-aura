package com.workora.identity.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

class KeycloakServiceAdminRoleTest {
    @Test
    void appliesRealmAdminRoleToApprovedAccount() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        KeycloakService keycloak = new KeycloakService(
                builder,
                "http://keycloak",
                "workora",
                "workora-api",
                "http://localhost:3000",
                "admin",
                "password");

        server.expect(requestTo("http://keycloak/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"service-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/admin/realms/workora/users")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"id\":\"keycloak-user-id\"}]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/workora/roles/ADMIN"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"admin-role-id\",\"name\":\"ADMIN\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak/admin/realms/workora/users/keycloak-user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[{\"id\":\"admin-role-id\",\"name\":\"ADMIN\"}]"))
                .andRespond(withSuccess());

        keycloak.setRealmAdminRole("approved@example.com", true);

        server.verify();
    }
}
