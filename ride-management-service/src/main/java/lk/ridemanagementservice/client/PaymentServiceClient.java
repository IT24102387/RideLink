package lk.ridemanagementservice.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class PaymentServiceClient {

    private final WebClient paymentServiceWebClient;

    public PaymentServiceClient(@Qualifier("paymentServiceWebClient") WebClient paymentServiceWebClient) {
        this.paymentServiceWebClient = paymentServiceWebClient;
    }

    public Optional<FareEstimateResponseDto> estimateFare(Double distanceKm, Double durationMinutes, String vehicleType) {
        if (distanceKm == null || durationMinutes == null) {
            return Optional.empty();
        }

        FareEstimateRequestDto request = FareEstimateRequestDto.builder()
                .distanceKm(distanceKm)
                .durationMinutes(durationMinutes)
                .vehicleType(vehicleType)
                .build();

        try {
            FareEstimateResponseDto response = paymentServiceWebClient.post()
                    .uri("/api/v1/fares/estimate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FareEstimateResponseDto.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("Failed to fetch fare estimate from Payment Service: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<FareBreakdownDto> calculateFinalFare(String rideId, Double distanceKm, Double durationMinutes, String vehicleType) {
        CalculateFareRequestDto request = CalculateFareRequestDto.builder()
                .rideId(rideId)
                .distanceKm(distanceKm != null ? distanceKm : 0.0)
                .durationMinutes(durationMinutes != null ? durationMinutes : 0.0)
                .vehicleType(vehicleType)
                .build();

        try {
            FareBreakdownDto response = paymentServiceWebClient.post()
                    .uri("/api/v1/fares/calculate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FareBreakdownDto.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.error("Failed to calculate final fare from Payment Service for ride {}: {}", rideId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FareEstimateRequestDto {
        private Double distanceKm;
        private Double durationMinutes;
        private String vehicleType;
        private BigDecimal surgeMultiplier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FareEstimateResponseDto {
        private BigDecimal estimatedFare;
        private String currency;
        private FareBreakdownDto fareBreakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculateFareRequestDto {
        private String rideId;
        private Double distanceKm;
        private Double durationMinutes;
        private String vehicleType;
        private BigDecimal surgeMultiplier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FareBreakdownDto {
        private BigDecimal baseFare;
        private BigDecimal distanceFare;
        private BigDecimal timeFare;
        private BigDecimal surgeMultiplier;
        private BigDecimal totalFare;
        private String currency;
    }
}
