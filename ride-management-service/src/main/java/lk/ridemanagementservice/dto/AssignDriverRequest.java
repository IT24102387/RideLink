package lk.ridemanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignDriverRequest {

    @NotBlank(message = "Driver ID is required for assignment")
    private String driverId;
}
