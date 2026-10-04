package com.ridelink.fare_payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDto {

    private String serviceName;
    private String url;
    private boolean reachable;
    private String mode;
    private String message;
}
