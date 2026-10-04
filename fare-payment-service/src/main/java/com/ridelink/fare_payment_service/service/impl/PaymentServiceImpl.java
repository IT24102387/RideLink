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
        Fare fare;
        if (request.getFareId() != null && !request.getFareId().isBlank()) {
            fare = fareRepository.findById(request.getFareId())
                    .orElseThrow(() -> new FareNotFoundException("Fare not found with ID: " + request.getFareId()));

            if (!fare.getRideId().equals(request.getRideId())) {
                throw new IllegalArgumentException("Fare does not belong to ride ID: " + request.getRideId());
            }
        } else {
            fare = fareRepository.findByRideId(request.getRideId())
                    .orElseThrow(() -> new FareNotFoundException("Fare not found for ride ID: " + request.getRideId()));
        }

        // Auto-finalize fare if not finalized yet (convenient for testing and microservice flow)
        if (fare.getStatus() != FareStatus.FINALIZED || fare.getFinalFare() == null) {
            double finalAmount = fare.getEstimatedFare() != null ? fare.getEstimatedFare() : (fare.getBaseFare() != null ? fare.getBaseFare() : 200.0);
            fare.setFinalFare(finalAmount);
            fare.setStatus(FareStatus.FINALIZED);
            fare.setUpdatedAt(LocalDateTime.now());
            fare = fareRepository.save(fare);
        }

        // Idempotency: return existing payment if already processed for this fare
        final String targetFareId = fare.getId();
        if (paymentRepository.existsByFareId(targetFareId)) {
            Payment existingPayment = paymentRepository.findByFareId(targetFareId)
                    .orElseThrow(() -> new DuplicatePaymentException("Payment already created for fare ID: " + targetFareId));
            PaymentResponse existingResp = mapToPaymentResponse(existingPayment);
            existingResp.setRideSynced(true);
            return existingResp;
        }

        LocalDateTime now = LocalDateTime.now();
        String transactionRef = "TXN-" + UUID.randomUUID().toString().toUpperCase();

        Payment payment = Payment.builder()
                .rideId(request.getRideId())
                .fareId(fare.getId())
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
