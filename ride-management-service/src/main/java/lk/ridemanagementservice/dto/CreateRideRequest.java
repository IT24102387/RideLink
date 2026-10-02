package lk.ridemanagementservice.dto;

import jakarta.validation.constraints.NotNull;
import lk.ridemanagementservice.model.RideLocation;
import lk.ridemanagementservice.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRideRequest {

    private String passengerId;

    @NotNull(message = "Pickup location is required")
    private RideLocation pickupLocation;

    @NotNull(message = "Destination location is required")
    private RideLocation destinationLocation;

    @NotNull(message = "Requested vehicle type is required")
    private VehicleType requestedVehicleType;

    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double estimatedFare;
}
