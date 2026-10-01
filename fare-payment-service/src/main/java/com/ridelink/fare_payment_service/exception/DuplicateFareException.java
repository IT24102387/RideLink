package com.ridelink.fare_payment_service.exception;

public class DuplicateFareException extends RuntimeException {
    public DuplicateFareException(String message) {
        super(message);
    }
}
