package com.ridelink.fare_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDto {

    private String id;
    private String userId;
    private String licenseNumber;
    private String availabilityStatus;
    private String serviceArea;
    private Double rating;
    private Integer totalTrips;
    private Boolean isApproved;
}
