package ru.yandex.practicum.mybankfront.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@Configuration
@RequiredArgsConstructor
public class AccountsClientConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Bean
    public ErrorDecoder errorDecoder() {
        return new ErrorDecoder.Default();
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.NONE;
    }

    @Bean
    public RequestInterceptor bearerTokenInterceptor() {
        return requestTemplate -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                        oauth2Token.getAuthorizedClientRegistrationId(),
                        oauth2Token.getName()
                );
                OAuth2AccessToken accessToken = authorizedClient != null
                        ? authorizedClient.getAccessToken()
                        : null;
                if (accessToken != null) {
                    requestTemplate.header("Authorization",
                            "Bearer " + accessToken.getTokenValue());
                }
            }
        };
    }
}
