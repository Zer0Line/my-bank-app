package ru.yandex.practicum.cash.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashTokenValidatorTest {

    private static final String ISSUER = "http://localhost:8082/realms/bank-realm";
    private static final List<String> EXPECTED_AUDIENCES = List.of("bank-ui");

    private final SecurityConfig config = new SecurityConfig();

    private Jwt jwt(String issuer, List<String> audience, List<String> roles) {
        var builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (issuer != null) {
            builder.issuer(issuer);
        }
        if (audience != null) {
            builder.audience(audience);
        }
        if (roles != null) {
            builder.claim("realm_access", java.util.Map.of("roles", roles));
        }
        return builder.build();
    }

    @Test
    void bankUiTokenWithUserRole_isAccepted() {
        Jwt token = jwt(ISSUER, EXPECTED_AUDIENCES, List.of("USER"));
        OAuth2TokenValidatorResult result = config.jwtValidator(ISSUER, EXPECTED_AUDIENCES).validate(token);
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void cashServiceToken_isRejectedByCashResourceServer() {
        // Token minted for the accounts-service audience (what cash-service uses to call accounts)
        // must NOT be accepted by cash-service's own resource server (audience bank-ui).
        Jwt token = jwt(ISSUER, List.of("accounts-service"), List.of("ACCOUNTS_WRITE"));
        OAuth2TokenValidatorResult result = config.jwtValidator(ISSUER, EXPECTED_AUDIENCES).validate(token);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void wrongIssuer_isRejected() {
        Jwt token = jwt("http://evil.example.com/realms/bank-realm", EXPECTED_AUDIENCES, List.of("USER"));
        OAuth2TokenValidatorResult result = config.jwtValidator(ISSUER, EXPECTED_AUDIENCES).validate(token);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void tokenWithoutAudience_isRejected() {
        Jwt token = jwt(ISSUER, null, List.of("USER"));
        OAuth2TokenValidatorResult result = config.jwtValidator(ISSUER, EXPECTED_AUDIENCES).validate(token);
        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void authoritiesAreMappedFromRealmAccessRoles() {
        Jwt token = jwt(ISSUER, EXPECTED_AUDIENCES, List.of("USER", "EXTRA"));
        var authorities = config.jwtAuthenticationConverter().convert(token).getAuthorities();
        assertThat(authorities).containsExactlyInAnyOrder(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("USER"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("EXTRA"));
    }
}
