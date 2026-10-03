package lk.ridemanagementservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class DriverServiceClient {

    private final WebClient driverServiceWebClient;

    public DriverServiceClient(@Qualifier("driverServiceWebClient") WebClient driverServiceWebClient) {
        this.driverServiceWebClient = driverServiceWebClient;
    }

    public Optional<DriverDto> getDriverById(String driverId) {
        try {
            DriverDto driver = driverServiceWebClient.get()
                    .uri("/api/v1/drivers/{driverId}", driverId)
                    .retrieve()
                    .bodyToMono(DriverDto.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
            return Optional.ofNullable(driver);
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Driver not found in Driver Service: {}", driverId);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to query Driver Service for driverId {}: {}", driverId, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean updateDriverAvailability(String driverId, String availabilityStatus) {
        try {
            driverServiceWebClient.patch()
                    .uri("/api/v1/drivers/{driverId}/availability", driverId)
                    .bodyValue(Map.of("availabilityStatus", availabilityStatus))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(4))
                    .block();
            log.info("Successfully updated driver {} availability to {}", driverId, availabilityStatus);
            return true;
        } catch (Exception ex) {
            log.error("Failed to update availability for driver {}: {}", driverId, ex.getMessage());
            return false;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDto {
        private String id;
        private String userId;
        private String licenseNumber;
        private String availabilityStatus;
        private String serviceArea;
        private Boolean isApproved;
    }
}
