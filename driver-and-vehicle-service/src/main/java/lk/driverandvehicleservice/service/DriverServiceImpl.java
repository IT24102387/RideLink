package lk.driverandvehicleservice.service;

import lk.driverandvehicleservice.dto.DriverResponse;
import lk.driverandvehicleservice.dto.RegisterDriverRequest;
import lk.driverandvehicleservice.dto.UpdateAvailabilityRequest;
import lk.driverandvehicleservice.dto.UpdateLocationRequest;
import lk.driverandvehicleservice.dto.UpdateVehicleRequest;
import lk.driverandvehicleservice.exception.DriverAlreadyExistsException;
import lk.driverandvehicleservice.exception.DriverNotFoundException;
import lk.driverandvehicleservice.exception.InvalidDriverStateException;
import lk.driverandvehicleservice.exception.LicenseAlreadyRegisteredException;
import lk.driverandvehicleservice.exception.UnauthorizedActionException;
import lk.driverandvehicleservice.model.AvailabilityStatus;
import lk.driverandvehicleservice.model.Driver;
import lk.driverandvehicleservice.model.Location;
import lk.driverandvehicleservice.model.Vehicle;
import lk.driverandvehicleservice.model.VehicleType;
import lk.driverandvehicleservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import lk.driverandvehicleservice.client.AccountServiceClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverRepository driverRepository;
    private final AccountServiceClient accountServiceClient;

    @Override
    public DriverResponse registerDriver(RegisterDriverRequest request, String authenticatedUserId) {
        String effectiveUserId = request.getUserId() != null && !request.getUserId().isBlank()
                ? request.getUserId()
                : authenticatedUserId;

        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            throw new IllegalArgumentException("User ID is required to register driver operational profile");
        }

        // Verify account exists in Account Service
        accountServiceClient.getAccountById(effectiveUserId).ifPresent(account -> {
            log.info("Verified account for driver registration: email={}, role={}", account.getEmail(), account.getRole());
        });

        if (driverRepository.existsByUserId(effectiveUserId)) {
            throw new DriverAlreadyExistsException("A driver profile already exists for user ID: " + effectiveUserId);
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new LicenseAlreadyRegisteredException("Driving license number is already registered: " + request.getLicenseNumber());
        }

        Driver driver = Driver.builder()
                .userId(effectiveUserId)
                .licenseNumber(request.getLicenseNumber().trim())
                .serviceArea(request.getServiceArea().trim())
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .vehicle(request.getVehicle())
                .rating(5.0)
                .totalTrips(0)
                .isApproved(true)
                .build();

        Driver savedDriver = driverRepository.save(driver);
        log.info("Registered driver profile for userId: {}, driverId: {}", effectiveUserId, savedDriver.getId());
        return DriverResponse.from(savedDriver);
    }

    @Override
    public DriverResponse getDriverById(String driverId) {
        Driver driver = findDriverOrThrow(driverId);
        return DriverResponse.from(driver);
    }

    @Override
    public DriverResponse getDriverByUserId(String userId) {
        Driver driver = driverRepository.findByUserId(userId)
                .orElseThrow(() -> new DriverNotFoundException("Driver profile not found for user ID: " + userId));
        return DriverResponse.from(driver);
    }

    @Override
    public DriverResponse updateVehicle(String driverId, UpdateVehicleRequest request, String authenticatedUserId, boolean isAdmin) {
        Driver driver = findDriverOrThrow(driverId);
        verifyOwnership(driver, authenticatedUserId, isAdmin);

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate().trim().toUpperCase())
                .make(request.getMake().trim())
                .model(request.getModel().trim())
                .year(request.getYear())
                .color(request.getColor())
                .vehicleType(request.getVehicleType())
                .capacity(request.getCapacity() != null ? request.getCapacity() : 4)
                .build();

        driver.setVehicle(vehicle);
        Driver updatedDriver = driverRepository.save(driver);
        log.info("Updated vehicle details for driverId: {}", driverId);
        return DriverResponse.from(updatedDriver);
    }

    @Override
    public DriverResponse updateAvailability(String driverId, UpdateAvailabilityRequest request, String authenticatedUserId, boolean isAdmin) {
        Driver driver = findDriverOrThrow(driverId);
        verifyOwnership(driver, authenticatedUserId, isAdmin);

        if (request.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE) {
            if (driver.getVehicle() == null) {
                throw new InvalidDriverStateException("Cannot set status to AVAILABLE without registered vehicle details");
            }
            if (Boolean.FALSE.equals(driver.getIsApproved())) {
                throw new InvalidDriverStateException("Driver account is pending approval or suspended");
            }
        }

        driver.setAvailabilityStatus(request.getAvailabilityStatus());
        Driver updatedDriver = driverRepository.save(driver);
        log.info("Updated availability for driverId: {} to {}", driverId, request.getAvailabilityStatus());
        return DriverResponse.from(updatedDriver);
    }

    @Override
    public DriverResponse updateLocation(String driverId, UpdateLocationRequest request, String authenticatedUserId, boolean isAdmin) {
        Driver driver = findDriverOrThrow(driverId);
        verifyOwnership(driver, authenticatedUserId, isAdmin);

        Location location = Location.builder()
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .lastUpdated(Instant.now())
                .build();

        driver.setCurrentLocation(location);
        Driver updatedDriver = driverRepository.save(driver);
        return DriverResponse.from(updatedDriver);
    }

    @Override
    public List<DriverResponse> getAvailableDrivers(String serviceArea, VehicleType vehicleType) {
        List<Driver> drivers;

        if (serviceArea != null && !serviceArea.isBlank() && vehicleType != null) {
            drivers = driverRepository.findByAvailabilityStatusAndServiceAreaIgnoreCaseAndVehicle_VehicleType(
                    AvailabilityStatus.AVAILABLE, serviceArea.trim(), vehicleType
            );
        } else if (serviceArea != null && !serviceArea.isBlank()) {
            drivers = driverRepository.findByAvailabilityStatusAndServiceAreaIgnoreCase(
                    AvailabilityStatus.AVAILABLE, serviceArea.trim()
            );
        } else if (vehicleType != null) {
            drivers = driverRepository.findByAvailabilityStatusAndVehicle_VehicleType(
                    AvailabilityStatus.AVAILABLE, vehicleType
            );
        } else {
            drivers = driverRepository.findByAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        return drivers.stream()
                .map(DriverResponse::from)
                .collect(Collectors.toList());
    }

    private Driver findDriverOrThrow(String driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new DriverNotFoundException("Driver not found with ID: " + driverId));
    }

    private void verifyOwnership(Driver driver, String authenticatedUserId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (authenticatedUserId == null || (!authenticatedUserId.equals(driver.getUserId()) && !authenticatedUserId.equals(driver.getId()))) {
            throw new UnauthorizedActionException("You are not authorized to modify this driver profile");
        }
    }
}
