package lk.driverandvehicleservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Value("${services.account-service.url:http://localhost:8080}")
    private String accountServiceUrl;

    @Value("${services.ride-service.url:http://localhost:8082}")
    private String rideServiceUrl;

    @Value("${services.payment-service.url:http://localhost:8083}")
    private String paymentServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(jwtForwardingFilter());
    }

    @Bean
    public WebClient accountServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(accountServiceUrl).build();
    }

    @Bean
    public WebClient rideServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(rideServiceUrl).build();
    }

    @Bean
    public WebClient paymentServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(paymentServiceUrl).build();
    }

    private ExchangeFilterFunction jwtForwardingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && !authHeader.isBlank() && clientRequest.headers().getFirst(HttpHeaders.AUTHORIZATION) == null) {
                    ClientRequest authorizedRequest = ClientRequest.from(clientRequest)
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .build();
                    return Mono.just(authorizedRequest);
                }
            }
            return Mono.just(clientRequest);
        });
    }
}
