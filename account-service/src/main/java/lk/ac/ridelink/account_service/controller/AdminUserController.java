package lk.ac.ridelink.account_service.controller;

import jakarta.validation.Valid;
import lk.ac.ridelink.account_service.dto.UpdateRoleRequest;
import lk.ac.ridelink.account_service.dto.UpdateStatusRequest;
import lk.ac.ridelink.account_service.dto.UserResponse;
import lk.ac.ridelink.account_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable String userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.updateRole(userId, request));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable String userId,
            @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(userService.updateStatus(userId, request));
    }
}
