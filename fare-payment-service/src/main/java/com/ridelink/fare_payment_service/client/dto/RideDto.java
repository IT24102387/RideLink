package com.ridelink.fare_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideDto {

    private String id;
    private String passengerId;
    private String driverId;
    private String status;
    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double estimatedFare;
    private Double finalFare;
    private String paymentId;
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
}
