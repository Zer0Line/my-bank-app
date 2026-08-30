package ru.yandex.practicum.transfer.client;

import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.transfer.exception.InsufficientFundsException;

public class AccountsServiceFeignConfig {

    @Bean
    public RequestInterceptor clientCredentialsInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return requestTemplate -> {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("service-client")
                    .principal("transfer-service")
                    .build();

            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to obtain client credentials token");
            }

            requestTemplate.header("Authorization",
                    "Bearer " + authorizedClient.getAccessToken().getTokenValue());
        };
    }

    @Bean
    public ErrorDecoder accountsServiceErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == HttpStatus.BAD_REQUEST.value()) {
                return new InsufficientFundsException("Insufficient funds");
            }
            return feign.FeignException.errorStatus(methodKey, response);
        };
    }
}
