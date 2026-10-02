package lk.driverandvehicleservice.dto;

import jakarta.validation.constraints.NotBlank;
import lk.driverandvehicleservice.model.Vehicle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDriverRequest {

    // Can be passed explicitly (e.g. by admin or onboarding) or resolved from JWT token
    private String userId;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Service area is required")
    private String serviceArea;

    private Vehicle vehicle;
}
