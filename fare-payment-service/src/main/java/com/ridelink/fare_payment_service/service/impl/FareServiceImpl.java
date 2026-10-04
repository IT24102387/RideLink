package com.ridelink.fare_payment_service.service.impl;

import com.ridelink.fare_payment_service.dto.*;
import com.ridelink.fare_payment_service.exception.DuplicateFareException;
import com.ridelink.fare_payment_service.exception.FareNotFoundException;
import com.ridelink.fare_payment_service.model.Fare;
import com.ridelink.fare_payment_service.model.FareStatus;
import com.ridelink.fare_payment_service.repository.FareRepository;
import com.ridelink.fare_payment_service.service.FareService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FareServiceImpl implements FareService {

    private static final double DEFAULT_BASE_FARE = 200.00;
    private static final double DEFAULT_DISTANCE_RATE = 100.00; // per km
    private static final double DEFAULT_TIME_RATE = 20.00;      // per minute
    private static final String DEFAULT_CURRENCY = "LKR";

    private final FareRepository fareRepository;

    public FareServiceImpl(FareRepository fareRepository) {
        this.fareRepository = fareRepository;
    }

    @Override
    public FareResponse estimateFare(FareEstimateRequest request) {
        final String requestedRideId = request.getRideId();

        // If ride ID is provided and already exists, return existing or update
        if (requestedRideId != null && !requestedRideId.isBlank()) {
            if (fareRepository.existsByRideId(requestedRideId)) {
                Fare existingFare = fareRepository.findByRideId(requestedRideId)
                        .orElseThrow(() -> new FareNotFoundException("Fare not found for ride ID: " + requestedRideId));
                return mapToFareResponse(existingFare);
            }
        }

        final String effectiveRideId = (requestedRideId != null && !requestedRideId.isBlank())
                ? requestedRideId
                : "EST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        String vehicleType = resolveVehicleType(request.getVehicleType());
        double vehicleMultiplier = getVehicleMultiplier(vehicleType);
        double surgeMultiplier = resolveSurgeMultiplier(request.getSurgeMultiplier());

        double baseFare = round(DEFAULT_BASE_FARE * vehicleMultiplier);
        double distanceFare = round(request.getDistanceKm() * DEFAULT_DISTANCE_RATE * vehicleMultiplier);
        double timeFare = round(request.getDurationMinutes() * DEFAULT_TIME_RATE * vehicleMultiplier);
        double subtotal = baseFare + distanceFare + timeFare;
        double estimatedFare = round(subtotal * surgeMultiplier);

        LocalDateTime now = LocalDateTime.now();

        Fare fare = Fare.builder()
                .rideId(effectiveRideId)
                .distanceKm(request.getDistanceKm())
                .durationMinutes(request.getDurationMinutes())
                .vehicleType(vehicleType)
                .surgeMultiplier(surgeMultiplier)
                .baseFare(baseFare)
                .distanceFare(distanceFare)
                .timeFare(timeFare)
                .estimatedFare(estimatedFare)
                .finalFare(null)
                .currency(DEFAULT_CURRENCY)
                .status(FareStatus.ESTIMATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Fare savedFare = fareRepository.save(fare);
        return mapToFareResponse(savedFare);
    }

    @Override
    public CalculateFareResponse calculateFare(CalculateFareRequest request) {
        String vehicleType = resolveVehicleType(request.getVehicleType());
        double vehicleMultiplier = getVehicleMultiplier(vehicleType);
        double surgeMultiplier = resolveSurgeMultiplier(request.getSurgeMultiplier());

        double baseFare = round(DEFAULT_BASE_FARE * vehicleMultiplier);
        double distanceFare = round(request.getDistanceKm() * DEFAULT_DISTANCE_RATE * vehicleMultiplier);
        double timeFare = round(request.getDurationMinutes() * DEFAULT_TIME_RATE * vehicleMultiplier);
        double subtotal = baseFare + distanceFare + timeFare;
        double totalFare = round(subtotal * surgeMultiplier);

        String fareId = null;
        if (request.getRideId() != null && !request.getRideId().isBlank()) {
            Fare fare = fareRepository.findByRideId(request.getRideId())
                    .orElse(null);

            LocalDateTime now = LocalDateTime.now();
            if (fare == null) {
                fare = Fare.builder()
                        .rideId(request.getRideId())
                        .distanceKm(request.getDistanceKm())
                        .durationMinutes(request.getDurationMinutes())
                        .vehicleType(vehicleType)
                        .surgeMultiplier(surgeMultiplier)
                        .baseFare(baseFare)
                        .distanceFare(distanceFare)
                        .timeFare(timeFare)
                        .estimatedFare(totalFare)
                        .finalFare(totalFare)
                        .currency(DEFAULT_CURRENCY)
                        .status(FareStatus.FINALIZED)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
            } else {
                fare.setDistanceKm(request.getDistanceKm());
                fare.setDurationMinutes(request.getDurationMinutes());
                fare.setVehicleType(vehicleType);
                fare.setSurgeMultiplier(surgeMultiplier);
                fare.setBaseFare(baseFare);
                fare.setDistanceFare(distanceFare);
                fare.setTimeFare(timeFare);
                fare.setFinalFare(totalFare);
                fare.setStatus(FareStatus.FINALIZED);
                fare.setUpdatedAt(now);
            }
            Fare savedFare = fareRepository.save(fare);
            fareId = savedFare.getId();
        }

        return CalculateFareResponse.builder()
                .rideId(request.getRideId())
                .fareId(fareId)
                .baseFare(baseFare)
                .distanceFare(distanceFare)
                .timeFare(timeFare)
                .surgeMultiplier(surgeMultiplier)
                .totalFare(totalFare)
                .currency(DEFAULT_CURRENCY)
                .status("FINALIZED")
                .build();
    }

    @Override
    public FareResponse finalizeFare(String fareId, FinalFareRequest request) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new FareNotFoundException("Fare not found with ID: " + fareId));

        String vehicleType = resolveVehicleType(fare.getVehicleType());
        double vehicleMultiplier = getVehicleMultiplier(vehicleType);
        double surgeMultiplier = resolveSurgeMultiplier(fare.getSurgeMultiplier());

        double baseFare = round(DEFAULT_BASE_FARE * vehicleMultiplier);
        double distanceFare = round(request.getDistanceKm() * DEFAULT_DISTANCE_RATE * vehicleMultiplier);
        double timeFare = round(request.getDurationMinutes() * DEFAULT_TIME_RATE * vehicleMultiplier);
        double subtotal = baseFare + distanceFare + timeFare;
        double finalFare = round(subtotal * surgeMultiplier);

        fare.setDistanceKm(request.getDistanceKm());
        fare.setDurationMinutes(request.getDurationMinutes());
        fare.setBaseFare(baseFare);
        fare.setDistanceFare(distanceFare);
        fare.setTimeFare(timeFare);
        fare.setFinalFare(finalFare);
        fare.setStatus(FareStatus.FINALIZED);
        fare.setUpdatedAt(LocalDateTime.now());

        Fare updatedFare = fareRepository.save(fare);
        return mapToFareResponse(updatedFare);
    }

    @Override
    public FareResponse getFareById(String fareId) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new FareNotFoundException("Fare not found with ID: " + fareId));
        return mapToFareResponse(fare);
    }

    @Override
    public FareResponse getFareByRideId(String rideId) {
        Fare fare = fareRepository.findByRideId(rideId)
                .orElseThrow(() -> new FareNotFoundException("Fare not found for ride ID: " + rideId));
        return mapToFareResponse(fare);
    }

    private FareResponse mapToFareResponse(Fare fare) {
        double surge = resolveSurgeMultiplier(fare.getSurgeMultiplier());
        double total = fare.getFinalFare() != null ? fare.getFinalFare() : (fare.getEstimatedFare() != null ? fare.getEstimatedFare() : 0.0);
        String currency = fare.getCurrency() != null ? fare.getCurrency() : DEFAULT_CURRENCY;

        FareBreakdown breakdown = FareBreakdown.builder()
                .baseFare(fare.getBaseFare())
                .distanceFare(fare.getDistanceFare())
                .timeFare(fare.getTimeFare())
                .surgeMultiplier(surge)
                .totalFare(total)
                .currency(currency)
                .build();

        return FareResponse.builder()
                .id(fare.getId())
                .rideId(fare.getRideId())
                .distanceKm(fare.getDistanceKm())
                .durationMinutes(fare.getDurationMinutes())
                .vehicleType(fare.getVehicleType() != null ? fare.getVehicleType() : "CAR")
                .surgeMultiplier(surge)
                .baseFare(fare.getBaseFare())
                .distanceFare(fare.getDistanceFare())
                .timeFare(fare.getTimeFare())
                .estimatedFare(fare.getEstimatedFare())
                .finalFare(fare.getFinalFare())
                .currency(currency)
                .fareBreakdown(breakdown)
                .status(fare.getStatus())
                .createdAt(fare.getCreatedAt())
                .updatedAt(fare.getUpdatedAt())
                .build();
    }

    private String resolveVehicleType(String vehicleType) {
        return vehicleType != null && !vehicleType.isBlank() ? vehicleType.trim().toUpperCase() : "CAR";
    }

    private double getVehicleMultiplier(String vehicleType) {
        if (vehicleType == null) {
            return 1.0;
        }
        return switch (vehicleType.toUpperCase()) {
            case "BIKE", "MOTORCYCLE" -> 0.5;
            case "TUK_TUK", "THREE_WHEELER", "TUKTUK" -> 0.7;
            case "VAN" -> 1.4;
            default -> 1.0; // CAR, SEDAN, etc.
        };
    }

    private double resolveSurgeMultiplier(Double surgeMultiplier) {
        return (surgeMultiplier != null && surgeMultiplier > 0.0) ? surgeMultiplier : 1.0;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
