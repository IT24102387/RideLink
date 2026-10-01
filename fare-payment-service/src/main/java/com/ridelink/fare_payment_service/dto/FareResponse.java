package com.ridelink.fare_payment_service.dto;

import com.ridelink.fare_payment_service.model.FareStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FareResponse {

    private String id;
    private String rideId;
    private Double distanceKm;
    private Double durationMinutes;
    private Double baseFare;
    private Double distanceFare;
    private Double timeFare;
    private Double estimatedFare;
    private Double finalFare;
    private FareStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
