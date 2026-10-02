package lk.ridemanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rides")
public class Ride {

    @Id
    private String id;

    // Stable global passenger identifier from Account Service
    @Indexed
    private String passengerId;

    // Stable global driver identifier (assigned from Driver Service)
    @Indexed
    private String driverId;

    private RideLocation pickupLocation;
    private RideLocation destinationLocation;
    private VehicleType requestedVehicleType;

    @Indexed
    @Builder.Default
    private RideStatus status = RideStatus.REQUESTED;

    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double estimatedFare;
    private Double finalFare;

    // Cross-service reference populated after payment settlement
    private String paymentId;

    private String cancellationReason;
    private String cancelledBy;

    // Lifecycle timestamps
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
