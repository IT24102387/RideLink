package lk.ac.ridelink.account_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lk.ac.ridelink.account_service.dto.UpdateProfileRequest;
import lk.ac.ridelink.account_service.dto.UpdateRoleRequest;
import lk.ac.ridelink.account_service.dto.UpdateStatusRequest;
import lk.ac.ridelink.account_service.dto.UserResponse;
import lk.ac.ridelink.account_service.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.getProfile(jwt.getSubject()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.updateProfile(jwt.getSubject(), request));
    }

    @PutMapping("/me/role")
    public ResponseEntity<UserResponse> updateRole(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.updateRole(jwt.getSubject(), request));
    }

    @PutMapping("/me/status")
    public ResponseEntity<UserResponse> updateStatus(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.updateStatus(jwt.getSubject(), request));
    }
}
