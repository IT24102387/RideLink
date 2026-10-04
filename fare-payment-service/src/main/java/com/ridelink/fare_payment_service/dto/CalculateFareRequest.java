package com.ridelink.fare_payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalculateFareRequest {

    private String rideId;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance must be greater than or equal to 0")
    private Double distanceKm;

    @NotNull(message = "Duration is required")
    @DecimalMin(value = "0.0", message = "Duration must be greater than or equal to 0")
    private Double durationMinutes;

    private String vehicleType;

    private Double surgeMultiplier;
}
