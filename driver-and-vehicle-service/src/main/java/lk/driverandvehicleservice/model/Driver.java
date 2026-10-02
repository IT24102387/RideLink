package lk.driverandvehicleservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;

    // Stable global user identifier linking to Account Service (account of ROLE_DRIVER)
    @Indexed(unique = true)
    private String userId;

    @Indexed(unique = true)
    private String licenseNumber;

    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    @Indexed
    private String serviceArea;

    // Simulated real-time driver coordinates
    private Location currentLocation;

    // Operational vehicle details
    private Vehicle vehicle;

    @Builder.Default
    private Double rating = 5.0;

    @Builder.Default
    private Integer totalTrips = 0;

    @Builder.Default
    private Boolean isApproved = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
