package ru.yandex.practicum.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.notification.config.SecurityConfig;
import ru.yandex.practicum.notification.config.TestSecurityConfig;
import ru.yandex.practicum.notification.dto.OperationRequest;
import ru.yandex.practicum.notification.service.NotificationService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtAuthenticationToken tokenWithRoles(String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "testuser")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt, List.of(roles).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void addOperation_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperationRequest("testuser", "TEST", "test message", 100))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addOperation_withWrongRole_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperationRequest("testuser", "TEST", "test message", 100)))
                        .with(authentication(tokenWithRoles("WRONG_ROLE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addOperation_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OperationRequest("testuser", "TEST", "test message", 100)))
                        .with(authentication(tokenWithRoles("SERVICE"))))
                .andExpect(status().isOk());
    }

    @Test
    void addOperations_withCorrectRole_shouldSucceed() throws Exception {
        var requests = List.of(
                new OperationRequest("user1", "TYPE1", "msg1", 100),
                new OperationRequest("user2", "TYPE2", "msg2", 200)
        );
        mockMvc.perform(post("/api/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests))
                        .with(authentication(tokenWithRoles("SERVICE"))))
                .andExpect(status().isOk());
    }
}
