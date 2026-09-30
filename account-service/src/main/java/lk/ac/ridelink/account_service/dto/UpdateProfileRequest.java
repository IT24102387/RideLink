package lk.ac.ridelink.account_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters") String firstName,
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters") String lastName,
        @NotBlank(message = "Phone number is required")
        @Size(max = 32, message = "Phone number must not exceed 32 characters") String phoneNumber) {
    public UpdateProfileRequest {
        firstName = firstName == null ? null : firstName.strip();
        lastName = lastName == null ? null : lastName.strip();
        phoneNumber = phoneNumber == null ? null : phoneNumber.strip();
    }
}
