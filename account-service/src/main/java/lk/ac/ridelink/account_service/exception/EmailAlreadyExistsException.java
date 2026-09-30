package lk.ac.ridelink.account_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("An account with this email already exists");
    }
}
