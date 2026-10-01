package com.ridelink.fare_payment_service.exception;

public class FareNotFoundException extends RuntimeException {
    public FareNotFoundException(String message) {
        super(message);
    }
}
