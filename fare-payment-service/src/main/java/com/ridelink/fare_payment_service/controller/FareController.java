package com.ridelink.fare_payment_service.controller;

import com.ridelink.fare_payment_service.dto.*;
import com.ridelink.fare_payment_service.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/fares", "/api/fares"})
@Tag(name = "Fare Controller", description = "Endpoints for fare estimation, final calculation, and retrieval")
public class FareController {

    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate Fare", description = "Calculates estimated fare based on distance, duration, vehicle type, and surge multiplier. Called by Ride Management Service or mobile app.")
    public ResponseEntity<FareResponse> estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        FareResponse response = fareService.estimateFare(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate Final Fare Breakdown", description = "Calculates fare breakdown directly for Ride Management Service upon ride completion.")
    public ResponseEntity<CalculateFareResponse> calculateFare(@Valid @RequestBody CalculateFareRequest request) {
        CalculateFareResponse response = fareService.calculateFare(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{fareId}/finalize")
    @Operation(summary = "Finalize Fare", description = "Finalizes the fare using actual ride metrics before payment processing.")
    public ResponseEntity<FareResponse> finalizeFare(@PathVariable String fareId,
                                                     @Valid @RequestBody FinalFareRequest request) {
        FareResponse response = fareService.finalizeFare(fareId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fareId}")
    @Operation(summary = "Get Fare By ID", description = "Retrieves fare details by fare ID.")
    public ResponseEntity<FareResponse> getFareById(@PathVariable String fareId) {
        FareResponse response = fareService.getFareById(fareId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get Fare By Ride ID", description = "Retrieves fare details by associated ride ID.")
    public ResponseEntity<FareResponse> getFareByRideId(@PathVariable String rideId) {
        FareResponse response = fareService.getFareByRideId(rideId);
        return ResponseEntity.ok(response);
    }
}
