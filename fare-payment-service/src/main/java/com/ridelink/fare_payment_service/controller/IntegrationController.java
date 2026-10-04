package com.ridelink.fare_payment_service.controller;

import com.ridelink.fare_payment_service.client.AccountServiceClient;
import com.ridelink.fare_payment_service.client.DriverServiceClient;
import com.ridelink.fare_payment_service.client.RideServiceClient;
import com.ridelink.fare_payment_service.client.dto.RideDto;
import com.ridelink.fare_payment_service.client.dto.ServiceHealthDto;
import com.ridelink.fare_payment_service.dto.*;
import com.ridelink.fare_payment_service.model.PaymentMethod;
import com.ridelink.fare_payment_service.service.FareService;
import com.ridelink.fare_payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/integration", "/api/integration"})
@Tag(name = "Integration Controller", description = "Endpoints for checking connectivity and testing workflows with peer microservices")
public class IntegrationController {

    private final RideServiceClient rideServiceClient;
    private final AccountServiceClient accountServiceClient;
    private final DriverServiceClient driverServiceClient;
    private final FareService fareService;
    private final PaymentService paymentService;
    private final MongoTemplate mongoTemplate;

    public IntegrationController(RideServiceClient rideServiceClient,
                                 AccountServiceClient accountServiceClient,
                                 DriverServiceClient driverServiceClient,
                                 FareService fareService,
                                 PaymentService paymentService,
                                 MongoTemplate mongoTemplate) {
        this.rideServiceClient = rideServiceClient;
        this.accountServiceClient = accountServiceClient;
        this.driverServiceClient = driverServiceClient;
        this.fareService = fareService;
        this.paymentService = paymentService;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/status")
    @Operation(summary = "Check Microservices Connectivity", description = "Reports the reachability status of Ride Management, Account, Driver services, and MongoDB.")
    public ResponseEntity<Map<String, Object>> checkIntegrationStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("service", "fare-payment-service");
        result.put("status", "UP");

        // Database status
        Map<String, Object> dbStatus = new HashMap<>();
        try {
            String dbName = mongoTemplate.getDb().getName();
            dbStatus.put("status", "CONNECTED");
            dbStatus.put("databaseName", dbName);
        } catch (Exception e) {
            dbStatus.put("status", "ERROR");
            dbStatus.put("error", e.getMessage());
        }
        result.put("database", dbStatus);

        // Peer services status
        Map<String, ServiceHealthDto> peerServices = new HashMap<>();
        peerServices.put("rideManagementService", rideServiceClient.checkHealth());
        peerServices.put("accountService", accountServiceClient.checkHealth());
        peerServices.put("driverService", driverServiceClient.checkHealth());
        result.put("peerMicroservices", peerServices);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/rides/{rideId}")
    @Operation(summary = "Fetch Ride from Ride Management Service", description = "Tests inter-service call to Ride Management Service to retrieve ride details.")
    public ResponseEntity<Map<String, Object>> getRideFromRideService(@PathVariable String rideId) {
        Map<String, Object> response = new HashMap<>();
        response.put("rideId", rideId);
        response.put("targetServiceUrl", rideServiceClient.getRideServiceUrl());

        var rideOpt = rideServiceClient.getRideById(rideId);
        if (rideOpt.isPresent()) {
            response.put("status", "FOUND_IN_RIDE_SERVICE");
            response.put("ride", rideOpt.get());
        } else {
            response.put("status", "NOT_FOUND_OR_SERVICE_OFFLINE");
            response.put("message", "Could not fetch ride from Ride Service. Either the service is offline or ride ID does not exist.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/simulate-complete-flow")
    @Operation(summary = "Simulate End-to-End Fare & Payment Flow", description = "Simulates the complete cycle: Fare Estimate -> Fare Finalize/Calculate -> Payment -> Ride Sync.")
    public ResponseEntity<Map<String, Object>> simulateCompleteFlow(
            @RequestParam(defaultValue = "CAR") String vehicleType,
            @RequestParam(defaultValue = "15.0") Double distanceKm,
            @RequestParam(defaultValue = "30.0") Double durationMinutes,
            @RequestParam(defaultValue = "1.0") Double surgeMultiplier) {

        String testRideId = "SIM-RIDE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> workflowReport = new HashMap<>();
        workflowReport.put("simulatedRideId", testRideId);
        workflowReport.put("vehicleType", vehicleType);

        // Step 1: Estimate Fare
        FareEstimateRequest estimateReq = FareEstimateRequest.builder()
                .rideId(testRideId)
                .distanceKm(distanceKm)
                .durationMinutes(durationMinutes)
                .vehicleType(vehicleType)
                .surgeMultiplier(surgeMultiplier)
                .build();
        FareResponse estimatedFare = fareService.estimateFare(estimateReq);
        workflowReport.put("step1_fareEstimate", estimatedFare);

        // Step 2: Finalize Fare
        FinalFareRequest finalReq = new FinalFareRequest(distanceKm, durationMinutes);
        FareResponse finalizedFare = fareService.finalizeFare(estimatedFare.getId(), finalReq);
        workflowReport.put("step2_fareFinalized", finalizedFare);

        // Step 3: Process Payment
        CreatePaymentRequest paymentReq = CreatePaymentRequest.builder()
                .rideId(testRideId)
                .fareId(finalizedFare.getId())
                .paymentMethod(PaymentMethod.CARD)
                .build();
        PaymentResponse paymentResponse = paymentService.createPayment(paymentReq);
        workflowReport.put("step3_paymentCreated", paymentResponse);

        workflowReport.put("overallStatus", "SUCCESS");
        workflowReport.put("message", "Complete Fare & Payment flow simulated and persisted in MongoDB.");

        return ResponseEntity.ok(workflowReport);
    }
}
