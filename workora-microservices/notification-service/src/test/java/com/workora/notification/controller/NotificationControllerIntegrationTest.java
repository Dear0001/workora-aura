package com.workora.notification.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.notification.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Jwt tokenFor(UUID userId, String email) {
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "email", email));
    }

    @Test
    void unauthenticated_inbox_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateNotificationWithoutTrustedInternalToken() throws Exception {
        UUID userId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId.toString(),
                "type", "TASK_ASSIGNED",
                "title", "Task assigned to you",
                "body", "Please review the submission"));

        mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt().jwt(tokenFor(userId, "user@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void trustedServiceCanCreateNotificationForUser_thenUserSeesItInInbox() throws Exception {
        UUID userId = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", userId.toString(),
                "type", "TASK_ASSIGNED",
                "title", "Task assigned to you",
                "body", "Please review the submission"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt().jwt(tokenFor(userId, "user@acme.test")))
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.title").value("Task assigned to you"))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String notificationId = created.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/notifications")
                        .with(jwt().jwt(tokenFor(userId, "user@acme.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + notificationId + "')]").exists());
    }

    @Test
    void userCannotGetAnotherUsersNotification() throws Exception {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();

        String body = objectMapper.writeValueAsString(Map.of(
                "userId", ownerA.toString(),
                "type", "SYSTEM",
                "title", "Private to A"));

        MvcResult createResult = mockMvc.perform(post("/api/v1/notifications")
                        .with(jwt().jwt(tokenFor(ownerA, "a@acme.test")))
                        .header("X-Internal-Service-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String notificationId = created.path("data").path("id").asText();
        assertThat(notificationId).isNotBlank();

        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .with(jwt().jwt(tokenFor(ownerB, "b@acme.test"))))
                .andExpect(status().isNotFound());
    }
}
