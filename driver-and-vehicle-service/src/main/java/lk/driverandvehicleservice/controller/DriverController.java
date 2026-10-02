package lk.driverandvehicleservice.controller;

import jakarta.validation.Valid;
import lk.driverandvehicleservice.dto.DriverResponse;
import lk.driverandvehicleservice.dto.RegisterDriverRequest;
import lk.driverandvehicleservice.dto.UpdateAvailabilityRequest;
import lk.driverandvehicleservice.dto.UpdateLocationRequest;
import lk.driverandvehicleservice.dto.UpdateVehicleRequest;
import lk.driverandvehicleservice.model.VehicleType;
import lk.driverandvehicleservice.service.DriverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/drivers", "/drivers"})
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @PostMapping
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> registerDriver(@Valid @RequestBody RegisterDriverRequest request,
                                                         Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        DriverResponse response = driverService.registerDriver(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverResponse> getDriverById(@PathVariable String driverId) {
        DriverResponse response = driverService.getDriverById(driverId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<DriverResponse> getDriverByUserId(@PathVariable String userId) {
        DriverResponse response = driverService.getDriverByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{driverId}/vehicle")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateVehicle(@PathVariable String driverId,
                                                        @Valid @RequestBody UpdateVehicleRequest request,
                                                        Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        DriverResponse response = driverService.updateVehicle(driverId, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{driverId}/availability")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateAvailability(@PathVariable String driverId,
                                                             @Valid @RequestBody UpdateAvailabilityRequest request,
                                                             Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        DriverResponse response = driverService.updateAvailability(driverId, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{driverId}/location")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<DriverResponse> updateLocation(@PathVariable String driverId,
                                                         @Valid @RequestBody UpdateLocationRequest request,
                                                         Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        DriverResponse response = driverService.updateLocation(driverId, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DriverResponse>> getAvailableDrivers(
            @RequestParam(required = false) String serviceArea,
            @RequestParam(required = false) VehicleType vehicleType) {
        List<DriverResponse> response = driverService.getAvailableDrivers(serviceArea, vehicleType);
        return ResponseEntity.ok(response);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
