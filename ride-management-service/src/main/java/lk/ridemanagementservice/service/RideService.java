package lk.ridemanagementservice.service;

import lk.ridemanagementservice.dto.AssignDriverRequest;
import lk.ridemanagementservice.dto.CancelRideRequest;
import lk.ridemanagementservice.dto.CompleteRideRequest;
import lk.ridemanagementservice.dto.CreateRideRequest;
import lk.ridemanagementservice.dto.RideResponse;
import lk.ridemanagementservice.model.RideStatusHistory;

import java.util.List;

public interface RideService {

    RideResponse createRide(CreateRideRequest request, String authenticatedUserId);

    RideResponse getRideById(String rideId);

    List<RideResponse> getRidesByPassenger(String passengerId);

    List<RideResponse> getRidesByDriver(String driverId);

    RideResponse assignDriver(String rideId, AssignDriverRequest request, String currentUserId, boolean isAdmin);

    RideResponse acceptRide(String rideId, String currentDriverId, boolean isAdmin);

    RideResponse startRide(String rideId, String currentDriverId, boolean isAdmin);

    RideResponse completeRide(String rideId, CompleteRideRequest request, String currentDriverId, boolean isAdmin);

    RideResponse cancelRide(String rideId, CancelRideRequest request, String currentUserId, boolean isAdmin);

    List<RideStatusHistory> getRideHistory(String rideId);
}
