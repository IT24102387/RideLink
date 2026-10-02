package lk.driverandvehicleservice.repository;

import lk.driverandvehicleservice.model.AvailabilityStatus;
import lk.driverandvehicleservice.model.Driver;
import lk.driverandvehicleservice.model.VehicleType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends MongoRepository<Driver, String> {

    Optional<Driver> findByUserId(String userId);

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    boolean existsByUserId(String userId);

    boolean existsByLicenseNumber(String licenseNumber);

    List<Driver> findByAvailabilityStatus(AvailabilityStatus status);

    List<Driver> findByAvailabilityStatusAndServiceAreaIgnoreCase(AvailabilityStatus status, String serviceArea);

    List<Driver> findByAvailabilityStatusAndServiceAreaIgnoreCaseAndVehicle_VehicleType(
            AvailabilityStatus status,
            String serviceArea,
            VehicleType vehicleType
    );

    List<Driver> findByAvailabilityStatusAndVehicle_VehicleType(
            AvailabilityStatus status,
            VehicleType vehicleType
    );
}
