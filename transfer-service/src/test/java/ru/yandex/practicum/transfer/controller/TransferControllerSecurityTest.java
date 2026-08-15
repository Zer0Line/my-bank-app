package ru.yandex.practicum.transfer.controller;

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
import ru.yandex.practicum.transfer.config.SecurityConfig;
import ru.yandex.practicum.transfer.config.TestSecurityConfig;
import ru.yandex.practicum.transfer.dto.TransferActionRequest;
import ru.yandex.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TransferController.class, properties = {
        "clients.notification-service.url=http://localhost:8085",
        "clients.accounts-service.url=http://localhost:9092"
})
@Import({SecurityConfig.class, TestSecurityConfig.class})
class TransferControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferService transferService;

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
    void transfer_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferActionRequest(BigDecimal.valueOf(500), "recipient"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transfer_withWrongRole_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferActionRequest(BigDecimal.valueOf(500), "recipient")))
                        .with(authentication(tokenWithRoles("WRONG_ROLE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void transfer_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TransferActionRequest(BigDecimal.valueOf(500), "recipient")))
                        .with(authentication(tokenWithRoles("TRANSFER_WRITE"))))
                .andExpect(status().isOk());
    }
}
