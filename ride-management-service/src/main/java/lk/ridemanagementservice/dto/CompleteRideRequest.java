package lk.ridemanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRideRequest {

    private Double actualDistanceKm;
    private Double actualDurationMinutes;
    private Double finalFare;
    private String paymentId;
}
