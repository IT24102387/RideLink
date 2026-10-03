package lk.ridemanagementservice.controller;

import jakarta.validation.Valid;
import lk.ridemanagementservice.dto.AssignDriverRequest;
import lk.ridemanagementservice.dto.CancelRideRequest;
import lk.ridemanagementservice.dto.CompleteRideRequest;
import lk.ridemanagementservice.dto.CreateRideRequest;
import lk.ridemanagementservice.dto.RideResponse;
import lk.ridemanagementservice.model.RideStatusHistory;
import lk.ridemanagementservice.service.RideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/rides", "/rides"})
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping
    @PreAuthorize("hasAnyRole('PASSENGER', 'ADMIN')")
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request,
                                                   Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        RideResponse response = rideService.createRide(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponse> getRideById(@PathVariable String rideId) {
        RideResponse response = rideService.getRideById(rideId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<RideResponse>> getRidesByPassenger(@PathVariable String passengerId) {
        List<RideResponse> response = rideService.getRidesByPassenger(passengerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RideResponse>> getRidesByDriver(@PathVariable String driverId) {
        List<RideResponse> response = rideService.getRidesByDriver(driverId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{rideId}/assign")
    public ResponseEntity<RideResponse> assignDriver(@PathVariable String rideId,
                                                     @Valid @RequestBody AssignDriverRequest request,
                                                     Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        RideResponse response = rideService.assignDriver(rideId, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/accept")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<RideResponse> acceptRide(@PathVariable String rideId,
                                                   Authentication authentication) {
        String currentDriverId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        RideResponse response = rideService.acceptRide(rideId, currentDriverId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/start")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<RideResponse> startRide(@PathVariable String rideId,
                                                  Authentication authentication) {
        String currentDriverId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        RideResponse response = rideService.startRide(rideId, currentDriverId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/complete")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    public ResponseEntity<RideResponse> completeRide(@PathVariable String rideId,
                                                     @RequestBody(required = false) CompleteRideRequest request,
                                                     Authentication authentication) {
        String currentDriverId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        RideResponse response = rideService.completeRide(rideId, request, currentDriverId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rideId}/cancel")
    public ResponseEntity<RideResponse> cancelRide(@PathVariable String rideId,
                                                   @Valid @RequestBody CancelRideRequest request,
                                                   Authentication authentication) {
        String currentUserId = authentication != null ? authentication.getName() : null;
        boolean isAdmin = isAdmin(authentication);
        RideResponse response = rideService.cancelRide(rideId, request, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rideId}/history")
    public ResponseEntity<List<RideStatusHistory>> getRideHistory(@PathVariable String rideId) {
        List<RideStatusHistory> response = rideService.getRideHistory(rideId);
        return ResponseEntity.ok(response);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
