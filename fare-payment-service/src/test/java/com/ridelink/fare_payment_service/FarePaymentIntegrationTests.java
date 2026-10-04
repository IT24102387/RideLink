package com.ridelink.fare_payment_service;

import com.ridelink.fare_payment_service.dto.*;
import com.ridelink.fare_payment_service.model.PaymentMethod;
import com.ridelink.fare_payment_service.model.PaymentStatus;
import com.ridelink.fare_payment_service.service.FareService;
import com.ridelink.fare_payment_service.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FarePaymentIntegrationTests {

    @Autowired
    private FareService fareService;

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("Should estimate fare with vehicle types and surge multiplier accurately")
    void shouldEstimateFareWithVehicleTypes() {
        String rideId = "TEST-RIDE-" + UUID.randomUUID().toString().substring(0, 8);

        // Car: Base 200, 10km * 100 = 1000, 20min * 20 = 400. Total = 1600
        FareEstimateRequest request = FareEstimateRequest.builder()
                .rideId(rideId)
                .distanceKm(10.0)
                .durationMinutes(20.0)
                .vehicleType("CAR")
                .surgeMultiplier(1.0)
                .build();

        FareResponse response = fareService.estimateFare(request);

        assertNotNull(response);
        assertEquals(rideId, response.getRideId());
        assertEquals(1600.0, response.getEstimatedFare());
        assertEquals("LKR", response.getCurrency());
        assertNotNull(response.getFareBreakdown());
        assertEquals(200.0, response.getFareBreakdown().getBaseFare());
        assertEquals(1000.0, response.getFareBreakdown().getDistanceFare());
        assertEquals(400.0, response.getFareBreakdown().getTimeFare());
        assertEquals(1600.0, response.getFareBreakdown().getTotalFare());
    }

    @Test
    @DisplayName("Should estimate fare without rideId (as called prior to ride creation)")
    void shouldEstimateFareWithoutRideId() {
        FareEstimateRequest request = FareEstimateRequest.builder()
                .distanceKm(5.0)
                .durationMinutes(15.0)
                .vehicleType("TUK_TUK")
                .surgeMultiplier(1.2)
                .build();

        FareResponse response = fareService.estimateFare(request);

        assertNotNull(response);
        assertTrue(response.getRideId().startsWith("EST-"));
        assertNotNull(response.getEstimatedFare());
        assertEquals("LKR", response.getCurrency());
    }

    @Test
    @DisplayName("Should calculate final fare breakdown matching ride-management-service requirements")
    void shouldCalculateFinalFareBreakdown() {
        String rideId = "TEST-RIDE-" + UUID.randomUUID().toString().substring(0, 8);

        CalculateFareRequest request = CalculateFareRequest.builder()
                .rideId(rideId)
                .distanceKm(12.0)
                .durationMinutes(25.0)
                .vehicleType("CAR")
                .surgeMultiplier(1.0)
                .build();

        CalculateFareResponse response = fareService.calculateFare(request);

        assertNotNull(response);
        assertEquals(rideId, response.getRideId());
        assertEquals(200.0, response.getBaseFare());
        assertEquals(1200.0, response.getDistanceFare());
        assertEquals(500.0, response.getTimeFare());
        assertEquals(1900.0, response.getTotalFare());
        assertEquals("LKR", response.getCurrency());
        assertEquals("FINALIZED", response.getStatus());
    }

    @Test
    @DisplayName("Should finalize fare and process payment with resilient peer service sync")
    void shouldFinalizeFareAndProcessPayment() {
        String rideId = "TEST-RIDE-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Estimate
        FareResponse estimated = fareService.estimateFare(
                FareEstimateRequest.builder()
                        .rideId(rideId)
                        .distanceKm(8.0)
                        .durationMinutes(15.0)
                        .vehicleType("CAR")
                        .build()
        );

        // 2. Finalize
        FinalFareRequest finalReq = new FinalFareRequest(8.5, 18.0);
        FareResponse finalized = fareService.finalizeFare(estimated.getId(), finalReq);

        assertNotNull(finalized);
        assertNotNull(finalized.getFinalFare());

        // 3. Payment
        CreatePaymentRequest payReq = CreatePaymentRequest.builder()
                .rideId(rideId)
                .fareId(finalized.getId())
                .paymentMethod(PaymentMethod.CARD)
                .build();

        PaymentResponse payment = paymentService.createPayment(payReq);

        assertNotNull(payment);
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals("LKR", payment.getCurrency());
        assertNotNull(payment.getTransactionReference());
        assertTrue(payment.getTransactionReference().startsWith("TXN-"));
    }

    @Autowired
    private com.ridelink.fare_payment_service.controller.IntegrationController integrationController;

    @Test
    @DisplayName("Should report status of peer microservices and database")
    void shouldReportIntegrationStatus() {
        var statusResponse = integrationController.checkIntegrationStatus();
        assertNotNull(statusResponse);
        assertEquals(200, statusResponse.getStatusCode().value());
        assertNotNull(statusResponse.getBody());
        assertEquals("UP", statusResponse.getBody().get("status"));
        assertTrue(statusResponse.getBody().containsKey("peerMicroservices"));
    }

    @Test
    @DisplayName("Should simulate end-to-end fare and payment workflow via IntegrationController")
    void shouldSimulateEndToEndFlow() {
        var flowResponse = integrationController.simulateCompleteFlow("CAR", 10.0, 20.0, 1.0);
        assertNotNull(flowResponse);
        assertEquals(200, flowResponse.getStatusCode().value());
        assertNotNull(flowResponse.getBody());
        assertEquals("SUCCESS", flowResponse.getBody().get("overallStatus"));
        assertTrue(flowResponse.getBody().containsKey("step1_fareEstimate"));
        assertTrue(flowResponse.getBody().containsKey("step2_fareFinalized"));
        assertTrue(flowResponse.getBody().containsKey("step3_paymentCreated"));
    }
}
