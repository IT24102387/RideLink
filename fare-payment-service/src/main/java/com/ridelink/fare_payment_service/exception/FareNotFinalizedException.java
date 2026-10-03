package com.ridelink.fare_payment_service.exception;

public class FareNotFinalizedException extends RuntimeException {
    public FareNotFinalizedException(String message) {
        super(message);
    }
}
