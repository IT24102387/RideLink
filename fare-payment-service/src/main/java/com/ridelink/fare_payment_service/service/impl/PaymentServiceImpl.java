package com.ridelink.fare_payment_service.service.impl;

import com.ridelink.fare_payment_service.client.RideServiceClient;
import com.ridelink.fare_payment_service.dto.CreatePaymentRequest;
import com.ridelink.fare_payment_service.dto.PaymentResponse;
import com.ridelink.fare_payment_service.exception.DuplicatePaymentException;
import com.ridelink.fare_payment_service.exception.FareNotFoundException;
import com.ridelink.fare_payment_service.exception.FareNotFinalizedException;
import com.ridelink.fare_payment_service.exception.PaymentNotFoundException;
import com.ridelink.fare_payment_service.model.Fare;
import com.ridelink.fare_payment_service.model.FareStatus;
import com.ridelink.fare_payment_service.model.Payment;
import com.ridelink.fare_payment_service.model.PaymentStatus;
import com.ridelink.fare_payment_service.repository.FareRepository;
import com.ridelink.fare_payment_service.repository.PaymentRepository;
import com.ridelink.fare_payment_service.service.PaymentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final FareRepository fareRepository;
    private final RideServiceClient rideServiceClient;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              FareRepository fareRepository,
                              RideServiceClient rideServiceClient) {
        this.paymentRepository = paymentRepository;
        this.fareRepository = fareRepository;
        this.rideServiceClient = rideServiceClient;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Fare fare = fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new FareNotFoundException("Fare not found with ID: " + request.getFareId()));

        if (!fare.getRideId().equals(request.getRideId())) {
            throw new IllegalArgumentException("Fare does not belong to ride ID: " + request.getRideId());
        }

        if (fare.getStatus() != FareStatus.FINALIZED) {
            throw new FareNotFinalizedException("Payment can only be processed for a FINALIZED fare");
        }

        if (paymentRepository.existsByFareId(request.getFareId())) {
            throw new DuplicatePaymentException("Payment already created for fare ID: " + request.getFareId());
        }

        LocalDateTime now = LocalDateTime.now();
        String transactionRef = "TXN-" + UUID.randomUUID().toString().toUpperCase();

        Payment payment = Payment.builder()
                .rideId(request.getRideId())
                .fareId(request.getFareId())
                .amount(fare.getFinalFare())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PAID)
                .transactionReference(transactionRef)
                .createdAt(now)
                .paidAt(now)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Sync with Ride Management Service (resilient - continues smoothly even if offline)
        boolean synced = rideServiceClient.notifyPaymentCompleted(
                request.getRideId(),
                savedPayment.getId(),
                savedPayment.getAmount()
        );

        PaymentResponse response = mapToPaymentResponse(savedPayment);
        response.setRideSynced(synced);
        return response;
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
        return mapToPaymentResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentByRideId(String rideId) {
        Payment payment = paymentRepository.findByRideId(rideId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for ride ID: " + rideId));
        return mapToPaymentResponse(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .rideId(payment.getRideId())
                .fareId(payment.getFareId())
                .amount(payment.getAmount())
                .currency("LKR")
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .transactionReference(payment.getTransactionReference())
                .rideSynced(false)
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
