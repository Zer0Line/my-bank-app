package ru.yandex.practicum.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RequestLoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public RequestLoggingGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            var token = request.getHeaders().getFirst("Authorization");
            log.info("Incoming request: {} {} | token={}",
                    request.getMethod(), request.getPath(),
                    token != null ? "Bearer " + token.substring(0, Math.min(20, token.length())) + "..." : "none");
            return chain.filter(exchange);
        };
    }
}
