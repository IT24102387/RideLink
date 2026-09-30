package lk.ac.ridelink.account_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateStatusRequest(
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|SUSPENDED|DISABLED",
                message = "Status must be ACTIVE, SUSPENDED or DISABLED") String status) {
}
