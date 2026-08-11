package ru.yandex.practicum.transfer.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter();

    private Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void convert_withRolesInRealmAccess_shouldMapThemToAuthorities() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("TRANSFER_WRITE", "SOME_OTHER_ROLE"))));

        var authorities = converter.convert(jwt).getAuthorities();

        assertEquals(List.of(
                new SimpleGrantedAuthority("TRANSFER_WRITE"),
                new SimpleGrantedAuthority("SOME_OTHER_ROLE")), authorities);
    }

    @Test
    void convert_withoutRealmAccess_shouldReturnEmptyAuthorities() {
        Jwt jwt = jwt(Map.of("preferred_username", "testuser"));

        assertTrue(converter.convert(jwt).getAuthorities().isEmpty());
    }

    @Test
    void convert_withRealmAccessWithoutRoles_shouldReturnEmptyAuthorities() {
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", null);
        Jwt jwt = jwt(Map.of("realm_access", realmAccess));

        assertTrue(converter.convert(jwt).getAuthorities().isEmpty());
    }

    @Test
    void convert_withEmptyRolesList_shouldReturnEmptyAuthorities() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of())));

        assertTrue(converter.convert(jwt).getAuthorities().isEmpty());
    }
}
