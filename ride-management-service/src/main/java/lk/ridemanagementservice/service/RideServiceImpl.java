package lk.ridemanagementservice.service;

import lk.ridemanagementservice.dto.AssignDriverRequest;
import lk.ridemanagementservice.dto.CancelRideRequest;
import lk.ridemanagementservice.dto.CompleteRideRequest;
import lk.ridemanagementservice.dto.CreateRideRequest;
import lk.ridemanagementservice.dto.RideResponse;
import lk.ridemanagementservice.exception.InvalidRideStateTransitionException;
import lk.ridemanagementservice.exception.RideNotFoundException;
import lk.ridemanagementservice.exception.UnauthorizedRideActionException;
import lk.ridemanagementservice.model.Ride;
import lk.ridemanagementservice.model.RideStatus;
import lk.ridemanagementservice.model.RideStatusHistory;
import lk.ridemanagementservice.repository.RideRepository;
import lk.ridemanagementservice.repository.RideStatusHistoryRepository;
import lk.ridemanagementservice.client.DriverServiceClient;
import lk.ridemanagementservice.client.PaymentServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideStatusHistoryRepository statusHistoryRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final DriverServiceClient driverServiceClient;

    @Override
    public RideResponse createRide(CreateRideRequest request, String authenticatedUserId) {
        String effectivePassengerId = request.getPassengerId() != null && !request.getPassengerId().isBlank()
                ? request.getPassengerId()
                : authenticatedUserId;

        if (effectivePassengerId == null || effectivePassengerId.isBlank()) {
            throw new IllegalArgumentException("Passenger ID is required to create a ride");
        }

        // Automatic fare estimation via Payment Service if not explicitly provided
        Double estimatedFare = request.getEstimatedFare();
        if (estimatedFare == null && request.getEstimatedDistanceKm() != null && request.getEstimatedDurationMinutes() != null) {
            String vehicleTypeName = request.getRequestedVehicleType() != null ? request.getRequestedVehicleType().name() : "CAR";
            estimatedFare = paymentServiceClient.estimateFare(
                    request.getEstimatedDistanceKm(),
                    request.getEstimatedDurationMinutes(),
                    vehicleTypeName
            ).map(res -> res.getEstimatedFare().doubleValue()).orElse(null);
        }

        Instant now = Instant.now();
        Ride ride = Ride.builder()
                .passengerId(effectivePassengerId)
                .pickupLocation(request.getPickupLocation())
                .destinationLocation(request.getDestinationLocation())
                .requestedVehicleType(request.getRequestedVehicleType())
                .status(RideStatus.REQUESTED)
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .estimatedFare(estimatedFare)
                .requestedAt(now)
                .build();

        Ride savedRide = rideRepository.save(ride);
        recordStatusHistory(savedRide.getId(), null, RideStatus.REQUESTED, effectivePassengerId, "Ride requested");
        log.info("Created ride with ID: {} for passenger: {}", savedRide.getId(), effectivePassengerId);
        return RideResponse.from(savedRide);
    }

    @Override
    public RideResponse getRideById(String rideId) {
        Ride ride = findRideOrThrow(rideId);
        return RideResponse.from(ride);
    }

    @Override
    public List<RideResponse> getRidesByPassenger(String passengerId) {
        return rideRepository.findByPassengerIdOrderByCreatedAtDesc(passengerId).stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<RideResponse> getRidesByDriver(String driverId) {
        return rideRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(RideResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public RideResponse assignDriver(String rideId, AssignDriverRequest request, String currentUserId, boolean isAdmin) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new InvalidRideStateTransitionException(
                    String.format("Cannot assign driver to ride in state: %s. Ride must be in REQUESTED state.", ride.getStatus())
            );
        }

        RideStatus oldStatus = ride.getStatus();
        String assignedDriverId = request.getDriverId().trim();
        driverServiceClient.getDriverById(assignedDriverId).ifPresent(driver ->
                log.info("Verified driver {} for ride {}: status={}", driver.getId(), rideId, driver.getAvailabilityStatus())
        );

        ride.setDriverId(assignedDriverId);
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(Instant.now());

        Ride updatedRide = rideRepository.save(ride);
        recordStatusHistory(rideId, oldStatus, RideStatus.ASSIGNED, currentUserId, "Driver assigned: " + assignedDriverId);
        log.info("Assigned driver {} to ride {}", assignedDriverId, rideId);
        return RideResponse.from(updatedRide);
    }

    @Override
    public RideResponse acceptRide(String rideId, String currentDriverId, boolean isAdmin) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.ASSIGNED) {
            throw new InvalidRideStateTransitionException(
                    String.format("Cannot accept ride in state: %s. Ride must be in ASSIGNED state.", ride.getStatus())
            );
        }

        verifyDriverOwnership(ride, currentDriverId, isAdmin);

        RideStatus oldStatus = ride.getStatus();
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());

        Ride updatedRide = rideRepository.save(ride);
        recordStatusHistory(rideId, oldStatus, RideStatus.ACCEPTED, currentDriverId, "Driver accepted ride");
        log.info("Driver {} accepted ride {}", currentDriverId, rideId);
        return RideResponse.from(updatedRide);
    }

    @Override
    public RideResponse startRide(String rideId, String currentDriverId, boolean isAdmin) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new InvalidRideStateTransitionException(
                    String.format("Cannot start ride in state: %s. Ride must be in ACCEPTED state.", ride.getStatus())
            );
        }

        verifyDriverOwnership(ride, currentDriverId, isAdmin);

        RideStatus oldStatus = ride.getStatus();
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());

        // Update driver availability to ON_TRIP via Driver Service
        if (ride.getDriverId() != null) {
            driverServiceClient.updateDriverAvailability(ride.getDriverId(), "ON_TRIP");
        }

        Ride updatedRide = rideRepository.save(ride);
        recordStatusHistory(rideId, oldStatus, RideStatus.IN_PROGRESS, currentDriverId, "Driver started trip");
        log.info("Trip started for ride {}", rideId);
        return RideResponse.from(updatedRide);
    }

    @Override
    public RideResponse completeRide(String rideId, CompleteRideRequest request, String currentDriverId, boolean isAdmin) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateTransitionException(
                    String.format("Cannot complete ride in state: %s. Ride must be in IN_PROGRESS state.", ride.getStatus())
            );
        }

        verifyDriverOwnership(ride, currentDriverId, isAdmin);

        RideStatus oldStatus = ride.getStatus();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());

        Double finalFare = request != null ? request.getFinalFare() : null;
        if (finalFare == null) {
            Double distance = request != null && request.getActualDistanceKm() != null
                    ? request.getActualDistanceKm()
                    : ride.getEstimatedDistanceKm();
            Double duration = request != null && request.getActualDurationMinutes() != null
                    ? request.getActualDurationMinutes()
                    : ride.getEstimatedDurationMinutes();
            String vehicleType = ride.getRequestedVehicleType() != null ? ride.getRequestedVehicleType().name() : "CAR";

            finalFare = paymentServiceClient.calculateFinalFare(rideId, distance, duration, vehicleType)
                    .map(fb -> fb.getTotalFare().doubleValue())
                    .orElse(ride.getEstimatedFare());
        }
        ride.setFinalFare(finalFare);

        if (request != null && request.getPaymentId() != null) {
            ride.setPaymentId(request.getPaymentId());
        }

        // Release driver back to AVAILABLE in Driver Service
        if (ride.getDriverId() != null) {
            driverServiceClient.updateDriverAvailability(ride.getDriverId(), "AVAILABLE");
        }

        Ride updatedRide = rideRepository.save(ride);
        recordStatusHistory(rideId, oldStatus, RideStatus.COMPLETED, currentDriverId, "Trip completed");
        log.info("Trip completed for ride {}", rideId);
        return RideResponse.from(updatedRide);
    }

    @Override
    public RideResponse cancelRide(String rideId, CancelRideRequest request, String currentUserId, boolean isAdmin) {
        Ride ride = findRideOrThrow(rideId);

        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new InvalidRideStateTransitionException("Cannot cancel an already COMPLETED ride");
        }
        if (ride.getStatus() == RideStatus.CANCELLED) {
            throw new InvalidRideStateTransitionException("Ride is already CANCELLED");
        }
        if (ride.getStatus() == RideStatus.IN_PROGRESS) {
            throw new InvalidRideStateTransitionException("Cannot cancel a ride that is already IN_PROGRESS");
        }

        verifyParticipantOrAdmin(ride, currentUserId, isAdmin);

        RideStatus oldStatus = ride.getStatus();
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancellationReason(request.getCancellationReason());
        ride.setCancelledBy(currentUserId != null ? currentUserId : "SYSTEM");
        ride.setCancelledAt(Instant.now());

        // If driver was assigned, release driver back to AVAILABLE
        if (ride.getDriverId() != null) {
            driverServiceClient.updateDriverAvailability(ride.getDriverId(), "AVAILABLE");
        }

        Ride updatedRide = rideRepository.save(ride);
        recordStatusHistory(rideId, oldStatus, RideStatus.CANCELLED, currentUserId, "Ride cancelled: " + request.getCancellationReason());
        log.info("Ride {} cancelled by {}", rideId, currentUserId);
        return RideResponse.from(updatedRide);
    }

    @Override
    public List<RideStatusHistory> getRideHistory(String rideId) {
        findRideOrThrow(rideId);
        return statusHistoryRepository.findByRideIdOrderByTimestampAsc(rideId);
    }

    private Ride findRideOrThrow(String rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found with ID: " + rideId));
    }

    private void verifyDriverOwnership(Ride ride, String currentDriverId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (currentDriverId == null || !currentDriverId.equals(ride.getDriverId())) {
            throw new UnauthorizedRideActionException("You are not the assigned driver for this ride");
        }
    }

    private void verifyParticipantOrAdmin(Ride ride, String currentUserId, boolean isAdmin) {
        if (isAdmin || currentUserId == null) {
            return;
        }
        boolean isPassenger = currentUserId.equals(ride.getPassengerId());
        boolean isDriver = currentUserId.equals(ride.getDriverId());
        if (!isPassenger && !isDriver) {
            throw new UnauthorizedRideActionException("You are not a participant in this ride");
        }
    }

    private void recordStatusHistory(String rideId, RideStatus previousStatus, RideStatus newStatus, String changedBy, String reason) {
        RideStatusHistory history = RideStatusHistory.builder()
                .rideId(rideId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        statusHistoryRepository.save(history);
    }
}
