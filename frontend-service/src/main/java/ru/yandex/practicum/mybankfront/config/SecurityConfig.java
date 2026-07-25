package ru.yandex.practicum.mybankfront.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .permitAll()
                        .clearAuthentication(true)
                );

        return http.build();
    }

    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
        ClientRegistrationRepository decorated = registrationId -> {
            ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(registrationId);
            if (reg != null && "keycloak".equals(registrationId)) {
                Map<String, Object> metadata = new HashMap<>();
                if (reg.getProviderDetails().getConfigurationMetadata() != null) {
                    metadata.putAll(reg.getProviderDetails().getConfigurationMetadata());
                }
                metadata.put("end_session_endpoint",
                        "http://localhost:8082/realms/bank-realm/protocol/openid-connect/logout");
                reg = ClientRegistration.withClientRegistration(reg)
                        .providerConfigurationMetadata(metadata)
                        .build();
            }
            return reg;
        };
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(decorated);
        handler.setPostLogoutRedirectUri("{baseUrl}");
        return handler;
    }
}
