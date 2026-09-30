package lk.ac.ridelink.account_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateRoleRequest(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "PASSENGER|DRIVER", message = "Role must be PASSENGER or DRIVER") String role) {
}
