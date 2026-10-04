package com.ridelink.fare_payment_service.client;

import com.ridelink.fare_payment_service.client.dto.DriverDto;
import com.ridelink.fare_payment_service.client.dto.ServiceHealthDto;
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
public class DriverServiceClient {

    private static final Logger log = LoggerFactory.getLogger(DriverServiceClient.class);

    private final RestClient restClient;
    private final String driverServiceUrl;

    public DriverServiceClient(@Value("${services.driver-service.url:http://localhost:8081}") String driverServiceUrl) {
        this.driverServiceUrl = driverServiceUrl;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(2500));

        this.restClient = RestClient.builder()
                .baseUrl(driverServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<DriverDto> getDriverById(String driverId) {
        if (driverId == null || driverId.isBlank()) {
            return Optional.empty();
        }

        try {
            DriverDto driver = restClient.get()
                    .uri("/api/v1/drivers/{driverId}", driverId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(DriverDto.class);
            return Optional.ofNullable(driver);
        } catch (Exception ex) {
            log.warn("[DriverServiceClient] Could not fetch driver {} from Driver Service at {}: {}. Fallback active.",
                    driverId, driverServiceUrl, ex.getMessage());
            return Optional.empty();
        }
    }

    public ServiceHealthDto checkHealth() {
        try {
            restClient.get()
                    .uri("/api/v1/drivers/available")
                    .retrieve()
                    .toBodilessEntity();
            return ServiceHealthDto.builder()
                    .serviceName("driver-and-vehicle-service")
                    .url(driverServiceUrl)
                    .reachable(true)
                    .mode("LIVE_CONNECTED")
                    .message("Driver Service is online")
                    .build();
        } catch (Exception ex) {
            return ServiceHealthDto.builder()
                    .serviceName("driver-and-vehicle-service")
                    .url(driverServiceUrl)
                    .reachable(false)
                    .mode("FALLBACK_RESILIENT")
                    .message("Offline or unreachable (" + ex.getClass().getSimpleName() + "). Fallback logic active.")
                    .build();
        }
    }

    public String getDriverServiceUrl() {
        return driverServiceUrl;
    }
}
