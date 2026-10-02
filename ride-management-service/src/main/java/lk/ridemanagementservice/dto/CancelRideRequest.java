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
public class CancelRideRequest {

    @NotBlank(message = "Cancellation reason must be provided")
    private String cancellationReason;
}
