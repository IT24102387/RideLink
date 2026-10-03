package lk.ridemanagementservice.config;

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

    @Value("${services.account-service.url}")
    private String accountServiceUrl;

    @Value("${services.driver-service.url}")
    private String driverServiceUrl;

    @Value("${services.payment-service.url}")
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
    public WebClient driverServiceWebClient(WebClient.Builder builder) {
        return builder.baseUrl(driverServiceUrl).build();
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
