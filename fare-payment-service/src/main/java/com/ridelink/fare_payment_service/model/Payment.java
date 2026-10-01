package com.ridelink.fare_payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;
    private String rideId;
    private String fareId;
    private Double amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private String transactionReference;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
