package lk.driverandvehicleservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    private String licensePlate;
    private String make;
    private String model;
    private Integer year;
    private String color;
    private VehicleType vehicleType;
    private Integer capacity;
}
