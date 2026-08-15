package ru.yandex.practicum.accounts.controller;

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
import ru.yandex.practicum.accounts.config.SecurityConfig;
import ru.yandex.practicum.accounts.config.TestSecurityConfig;
import ru.yandex.practicum.accounts.dto.CashAction;
import ru.yandex.practicum.accounts.dto.TransferRequest;
import ru.yandex.practicum.accounts.dto.UpdateAccountRequest;
import ru.yandex.practicum.accounts.dto.UpdateAmountRequest;
import ru.yandex.practicum.accounts.service.AccountsService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AccountsController.class, properties = "clients.notification-service.url=http://localhost:8085")
@Import({SecurityConfig.class, TestSecurityConfig.class})
class AccountsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountsService accountsService;

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
    void getAccount_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccount_withWrongRole_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .with(authentication(tokenWithRoles("WRONG_ROLE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAccount_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .with(authentication(tokenWithRoles("ACCOUNTS_WRITE"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateAccount_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(put("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAccountRequest("New Name", "2000-01-01")))
                        .with(authentication(tokenWithRoles("ACCOUNTS_WRITE"))))
                .andExpect(status().isOk());
    }

    @Test
    void updateAmount_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(patch("/api/accounts/amount")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateAmountRequest("testuser", BigDecimal.valueOf(500), CashAction.PUT)))
                        .with(authentication(tokenWithRoles("ACCOUNTS_WRITE"))))
                .andExpect(status().isOk());
    }

    @Test
    void transfer_withCorrectRole_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/accounts/transfer")
                        .header("Idempotency-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TransferRequest("sender", "recipient", BigDecimal.valueOf(200))))
                        .with(authentication(tokenWithRoles("ACCOUNTS_WRITE"))))
                .andExpect(status().isOk());
    }
}
