package ru.yandex.practicum.accounts.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigJwtValidatorTest {

    private static final String ISSUER = "http://localhost:8082/realms/bank-realm";
    private static final String AUDIENCE = "accounts-service";

    private final SecurityConfig securityConfig = new SecurityConfig();

    private Jwt jwt(String issuer, List<String> audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private OAuth2TokenValidatorResult validate(String issuer, List<String> audience) {
        return securityConfig.jwtValidator(ISSUER, List.of(AUDIENCE)).validate(jwt(issuer, audience));
    }

    @Test
    void validToken_passes() {
        OAuth2TokenValidatorResult result = validate(ISSUER, List.of(AUDIENCE));
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void wrongAudience_isRejected() {
        OAuth2TokenValidatorResult result = validate(ISSUER, List.of("bank-ui"));
        assertThat(result.hasErrors()).isTrue();
        OAuth2Error error = result.getErrors().iterator().next();
        assertThat(error.getDescription()).contains("aud claim");
    }

    @Test
    void wrongIssuer_isRejected() {
        OAuth2TokenValidatorResult result = validate("http://evil:8082/realms/bank-realm", List.of(AUDIENCE));
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void missingAudience_isRejected() {
        OAuth2TokenValidatorResult result = validate(ISSUER, List.of());
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void tokenForOtherServiceAudience_isRejectedByAccountsService() {
        OAuth2TokenValidatorResult result = validate(ISSUER, List.of("cash-service"));
        assertThat(result.hasErrors()).isTrue();
    }
}
