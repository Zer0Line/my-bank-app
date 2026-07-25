package ru.yandex.practicum.cash.controller;

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
import ru.yandex.practicum.cash.config.SecurityConfig;
import ru.yandex.practicum.cash.config.TestSecurityConfig;
import ru.yandex.practicum.cash.dto.CashAction;
import ru.yandex.practicum.cash.dto.CashActionRequest;
import ru.yandex.practicum.cash.service.CashService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashController.class)
@Import({SecurityConfig.class, TestSecurityConfig.class})
class CashControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CashService cashService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JwtAuthenticationToken tokenWithRoles(String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "testuser")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt, Stream.of(roles).map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    void editCash_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashActionRequest(500, CashAction.PUT))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void editCash_withWrongRole_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashActionRequest(500, CashAction.PUT)))
                        .with(authentication(tokenWithRoles("WRONG_ROLE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void editCash_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/cash")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashActionRequest(500, CashAction.PUT)))
                        .with(authentication(tokenWithRoles("SERVICE"))))
                .andExpect(status().isOk());
    }
}
