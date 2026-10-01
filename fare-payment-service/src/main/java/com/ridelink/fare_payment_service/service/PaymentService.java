package com.ridelink.fare_payment_service.service;

import com.ridelink.fare_payment_service.dto.CreatePaymentRequest;
import com.ridelink.fare_payment_service.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPaymentById(String paymentId);

    PaymentResponse getPaymentByRideId(String rideId);
}
