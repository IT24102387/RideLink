package lk.driverandvehicleservice.service;

import lk.driverandvehicleservice.dto.DriverResponse;
import lk.driverandvehicleservice.dto.RegisterDriverRequest;
import lk.driverandvehicleservice.dto.UpdateAvailabilityRequest;
import lk.driverandvehicleservice.dto.UpdateLocationRequest;
import lk.driverandvehicleservice.dto.UpdateVehicleRequest;
import lk.driverandvehicleservice.model.VehicleType;

import java.util.List;

public interface DriverService {

    DriverResponse registerDriver(RegisterDriverRequest request, String authenticatedUserId);

    DriverResponse getDriverById(String driverId);

    DriverResponse getDriverByUserId(String userId);

    DriverResponse updateVehicle(String driverId, UpdateVehicleRequest request, String authenticatedUserId, boolean isAdmin);

    DriverResponse updateAvailability(String driverId, UpdateAvailabilityRequest request, String authenticatedUserId, boolean isAdmin);

    DriverResponse updateLocation(String driverId, UpdateLocationRequest request, String authenticatedUserId, boolean isAdmin);

    List<DriverResponse> getAvailableDrivers(String serviceArea, VehicleType vehicleType);
}
