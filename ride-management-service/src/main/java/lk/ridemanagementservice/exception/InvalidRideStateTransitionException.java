package lk.ridemanagementservice.exception;

public class InvalidRideStateTransitionException extends RuntimeException {
    public InvalidRideStateTransitionException(String message) {
        super(message);
    }
}
