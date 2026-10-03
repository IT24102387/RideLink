package com.ridelink.fare_payment_service.service.impl;

import com.ridelink.fare_payment_service.dto.FareEstimateRequest;
import com.ridelink.fare_payment_service.dto.FareResponse;
import com.ridelink.fare_payment_service.dto.FinalFareRequest;
import com.ridelink.fare_payment_service.exception.DuplicateFareException;
import com.ridelink.fare_payment_service.exception.FareNotFoundException;
import com.ridelink.fare_payment_service.model.Fare;
import com.ridelink.fare_payment_service.model.FareStatus;
import com.ridelink.fare_payment_service.repository.FareRepository;
import com.ridelink.fare_payment_service.service.FareService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FareServiceImpl implements FareService {

    private static final double BASE_FARE = 200.00;
    private static final double DISTANCE_RATE = 100.00; // per km
    private static final double TIME_RATE = 20.00;      // per minute

    private final FareRepository fareRepository;

    public FareServiceImpl(FareRepository fareRepository) {
        this.fareRepository = fareRepository;
    }

    @Override
    public FareResponse estimateFare(FareEstimateRequest request) {
        if (fareRepository.existsByRideId(request.getRideId())) {
            throw new DuplicateFareException("Fare already estimated for ride ID: " + request.getRideId());
        }

        double distanceFare = request.getDistanceKm() * DISTANCE_RATE;
        double timeFare = request.getDurationMinutes() * TIME_RATE;
        double estimatedFare = BASE_FARE + distanceFare + timeFare;

        LocalDateTime now = LocalDateTime.now();

        Fare fare = Fare.builder()
                .rideId(request.getRideId())
                .distanceKm(request.getDistanceKm())
                .durationMinutes(request.getDurationMinutes())
                .baseFare(BASE_FARE)
                .distanceFare(distanceFare)
                .timeFare(timeFare)
                .estimatedFare(estimatedFare)
                .finalFare(null)
                .status(FareStatus.ESTIMATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Fare savedFare = fareRepository.save(fare);
        return mapToFareResponse(savedFare);
    }

    @Override
    public FareResponse finalizeFare(String fareId, FinalFareRequest request) {
        Fare fare = fareRepository.findById(fareId)
                .orElseThrow(() -> new FareNotFoundException("Fare not found with ID: " + fareId));

        double distanceFare = request.getDistanceKm() * DISTANCE_RATE;
        double timeFare = request.getDurationMinutes() * TIME_RATE;
        double finalFare = BASE_FARE + distanceFare + timeFare;

        fare.setDistanceKm(request.getDistanceKm());
        fare.setDurationMinutes(request.getDurationMinutes());
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
        return FareResponse.builder()
                .id(fare.getId())
                .rideId(fare.getRideId())
                .distanceKm(fare.getDistanceKm())
                .durationMinutes(fare.getDurationMinutes())
                .baseFare(fare.getBaseFare())
                .distanceFare(fare.getDistanceFare())
                .timeFare(fare.getTimeFare())
                .estimatedFare(fare.getEstimatedFare())
                .finalFare(fare.getFinalFare())
                .status(fare.getStatus())
                .createdAt(fare.getCreatedAt())
                .updatedAt(fare.getUpdatedAt())
                .build();
    }
}
