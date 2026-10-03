package com.ridelink.fare_payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FareEstimateRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    @NotNull(message = "Distance is required")
    @DecimalMin(value = "0.0", message = "Distance must be greater than or equal to 0")
    private Double distanceKm;

    @NotNull(message = "Duration is required")
    @DecimalMin(value = "0.0", message = "Duration must be greater than or equal to 0")
    private Double durationMinutes;

    public FareEstimateRequest() {
    }

    public FareEstimateRequest(String rideId, Double distanceKm, Double durationMinutes) {
        this.rideId = rideId;
        this.distanceKm = distanceKm;
        this.durationMinutes = durationMinutes;
    }

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Double durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
