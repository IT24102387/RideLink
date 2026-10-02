package lk.driverandvehicleservice.exception;

public class LicenseAlreadyRegisteredException extends RuntimeException {
    public LicenseAlreadyRegisteredException(String message) {
        super(message);
    }
}
