package lk.ridemanagementservice.exception;

public class UnauthorizedRideActionException extends RuntimeException {
    public UnauthorizedRideActionException(String message) {
        super(message);
    }
}
