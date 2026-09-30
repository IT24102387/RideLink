package lk.ac.ridelink.account_service.dto;

import java.time.LocalDateTime;
import lk.ac.ridelink.account_service.model.AccountStatus;
import lk.ac.ridelink.account_service.model.Role;

public record UserResponse(String id, String firstName, String lastName,
        String email, String phoneNumber, Role role, AccountStatus status,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
