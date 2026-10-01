package com.ridelink.fare_payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "fares")
public class Fare {

    @Id
    private String id;
    private String rideId;
    private Double distanceKm;
    private Double durationMinutes;
    private Double baseFare;
    private Double distanceFare;
    private Double timeFare;
    private Double estimatedFare;
    private Double finalFare;
    private FareStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
