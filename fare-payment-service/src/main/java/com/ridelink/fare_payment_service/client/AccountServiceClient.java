package com.ridelink.fare_payment_service.client;

import com.ridelink.fare_payment_service.client.dto.ServiceHealthDto;
import com.ridelink.fare_payment_service.client.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

@Component
public class AccountServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceClient.class);

    private final RestClient restClient;
    private final String accountServiceUrl;

    public AccountServiceClient(@Value("${services.account-service.url:http://localhost:8080}") String accountServiceUrl) {
        this.accountServiceUrl = accountServiceUrl;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(2500));

        this.restClient = RestClient.builder()
                .baseUrl(accountServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<UserDto> getUserProfile(String bearerToken) {
        try {
            UserDto user = restClient.get()
                    .uri("/api/users/me")
                    .header("Authorization", bearerToken != null && !bearerToken.startsWith("Bearer ") ? "Bearer " + bearerToken : bearerToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(UserDto.class);
            return Optional.ofNullable(user);
        } catch (Exception ex) {
            log.warn("[AccountServiceClient] Could not verify user with Account Service at {}: {}. Fallback active.",
                    accountServiceUrl, ex.getMessage());
            return Optional.empty();
        }
    }

    public ServiceHealthDto checkHealth() {
        try {
            restClient.get()
                    .uri("/api/users/me")
                    .retrieve()
                    .toBodilessEntity();
            return ServiceHealthDto.builder()
                    .serviceName("account-service")
                    .url(accountServiceUrl)
                    .reachable(true)
                    .mode("LIVE_CONNECTED")
                    .message("Account Service is online")
                    .build();
        } catch (Exception ex) {
            return ServiceHealthDto.builder()
                    .serviceName("account-service")
                    .url(accountServiceUrl)
                    .reachable(false)
                    .mode("FALLBACK_RESILIENT")
                    .message("Offline or requires auth (" + ex.getClass().getSimpleName() + "). Fallback logic active.")
                    .build();
        }
    }

    public String getAccountServiceUrl() {
        return accountServiceUrl;
    }
}
