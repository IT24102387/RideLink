package com.ridelink.fare_payment_service.service;

import com.ridelink.fare_payment_service.dto.CalculateFareRequest;
import com.ridelink.fare_payment_service.dto.CalculateFareResponse;
import com.ridelink.fare_payment_service.dto.FareEstimateRequest;
import com.ridelink.fare_payment_service.dto.FareResponse;
import com.ridelink.fare_payment_service.dto.FinalFareRequest;

public interface FareService {

    FareResponse estimateFare(FareEstimateRequest request);

    CalculateFareResponse calculateFare(CalculateFareRequest request);

    FareResponse finalizeFare(String fareId, FinalFareRequest request);

    FareResponse getFareById(String fareId);

    FareResponse getFareByRideId(String rideId);
}
