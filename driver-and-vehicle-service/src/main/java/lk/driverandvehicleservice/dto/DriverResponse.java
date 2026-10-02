package lk.driverandvehicleservice.dto;

import lk.driverandvehicleservice.model.AvailabilityStatus;
import lk.driverandvehicleservice.model.Driver;
import lk.driverandvehicleservice.model.Location;
import lk.driverandvehicleservice.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {

    private String id;
    private String userId;
    private String licenseNumber;
    private AvailabilityStatus availabilityStatus;
    private String serviceArea;
    private Location currentLocation;
    private Vehicle vehicle;
    private Double rating;
    private Integer totalTrips;
    private Boolean isApproved;
    private Instant createdAt;
    private Instant updatedAt;

    public static DriverResponse from(Driver driver) {
        if (driver == null) {
            return null;
        }
        return DriverResponse.builder()
                .id(driver.getId())
                .userId(driver.getUserId())
                .licenseNumber(driver.getLicenseNumber())
                .availabilityStatus(driver.getAvailabilityStatus())
                .serviceArea(driver.getServiceArea())
                .currentLocation(driver.getCurrentLocation())
                .vehicle(driver.getVehicle())
                .rating(driver.getRating())
                .totalTrips(driver.getTotalTrips())
                .isApproved(driver.getIsApproved())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build();
    }
}
