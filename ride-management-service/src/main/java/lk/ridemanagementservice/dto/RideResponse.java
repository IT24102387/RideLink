package lk.ridemanagementservice.dto;

import lk.ridemanagementservice.model.Ride;
import lk.ridemanagementservice.model.RideLocation;
import lk.ridemanagementservice.model.RideStatus;
import lk.ridemanagementservice.model.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private String id;
    private String passengerId;
    private String driverId;
    private RideLocation pickupLocation;
    private RideLocation destinationLocation;
    private VehicleType requestedVehicleType;
    private RideStatus status;
    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double estimatedFare;
    private Double finalFare;
    private String paymentId;
    private String cancellationReason;
    private String cancelledBy;
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static RideResponse from(Ride ride) {
        if (ride == null) {
            return null;
        }
        return RideResponse.builder()
                .id(ride.getId())
                .passengerId(ride.getPassengerId())
                .driverId(ride.getDriverId())
                .pickupLocation(ride.getPickupLocation())
                .destinationLocation(ride.getDestinationLocation())
                .requestedVehicleType(ride.getRequestedVehicleType())
                .status(ride.getStatus())
                .estimatedDistanceKm(ride.getEstimatedDistanceKm())
                .estimatedDurationMinutes(ride.getEstimatedDurationMinutes())
                .estimatedFare(ride.getEstimatedFare())
                .finalFare(ride.getFinalFare())
                .paymentId(ride.getPaymentId())
                .cancellationReason(ride.getCancellationReason())
                .cancelledBy(ride.getCancelledBy())
                .requestedAt(ride.getRequestedAt())
                .assignedAt(ride.getAssignedAt())
                .acceptedAt(ride.getAcceptedAt())
                .startedAt(ride.getStartedAt())
                .completedAt(ride.getCompletedAt())
                .cancelledAt(ride.getCancelledAt())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt())
                .build();
    }
}
