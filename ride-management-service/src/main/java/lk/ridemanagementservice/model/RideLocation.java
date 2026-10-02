package lk.ridemanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideLocation {

    private String address;
    private Double latitude;
    private Double longitude;
    private String city;
}
