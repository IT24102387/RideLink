package com.ridelink.fare_payment_service.client;

import com.ridelink.fare_payment_service.client.dto.RideDto;
import com.ridelink.fare_payment_service.client.dto.ServiceHealthDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class RideServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RideServiceClient.class);

    private final RestClient restClient;
    private final String rideServiceUrl;

    public RideServiceClient(@Value("${services.ride-service.url:http://localhost:8082}") String rideServiceUrl) {
        this.rideServiceUrl = rideServiceUrl;

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(2500));

        this.restClient = RestClient.builder()
                .baseUrl(rideServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<RideDto> getRideById(String rideId) {
        if (rideId == null || rideId.isBlank()) {
            return Optional.empty();
        }

        try {
            RideDto ride = restClient.get()
                    .uri("/api/v1/rides/{rideId}", rideId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(RideDto.class);
            return Optional.ofNullable(ride);
        } catch (Exception ex) {
            log.warn("[RideServiceClient] Could not fetch ride {} from Ride Service at {}: {}. Continuing with fallback mode.",
                    rideId, rideServiceUrl, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean notifyPaymentCompleted(String rideId, String paymentId, Double finalFare) {
        if (rideId == null || rideId.isBlank()) {
            return false;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("paymentId", paymentId);
        body.put("finalFare", finalFare);

        try {
            restClient.patch()
                    .uri("/api/v1/rides/{rideId}/complete", rideId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[RideServiceClient] Successfully synced payment {} with ride {}", paymentId, rideId);
            return true;
        } catch (Exception ex) {
            log.warn("[RideServiceClient] Ride Service at {} unreachable or ride update skipped: {}. Local payment recorded successfully.",
                    rideServiceUrl, ex.getMessage());
            return false;
        }
    }

    public ServiceHealthDto checkHealth() {
        try {
            restClient.get()
                    .uri("/api/v1/rides/health")
                    .retrieve()
                    .toBodilessEntity();
            return ServiceHealthDto.builder()
                    .serviceName("ride-management-service")
                    .url(rideServiceUrl)
                    .reachable(true)
                    .mode("LIVE_CONNECTED")
                    .message("Ride Management Service is online and reachable")
                    .build();
        } catch (Exception ex) {
            return ServiceHealthDto.builder()
                    .serviceName("ride-management-service")
                    .url(rideServiceUrl)
                    .reachable(false)
                    .mode("FALLBACK_RESILIENT")
                    .message("Offline or unreachable (" + ex.getClass().getSimpleName() + "). Fallback logic active.")
                    .build();
        }
    }

    public String getRideServiceUrl() {
        return rideServiceUrl;
    }
}
