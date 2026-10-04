package com.ridelink.fare_payment_service.dto;

import com.ridelink.fare_payment_service.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    @NotBlank(message = "Ride ID is required")
    private String rideId;

    private String fareId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
