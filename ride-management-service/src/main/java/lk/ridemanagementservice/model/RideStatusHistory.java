package lk.ridemanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ride_status_history")
public class RideStatusHistory {

    @Id
    private String id;

    @Indexed
    private String rideId;

    private RideStatus previousStatus;
    private RideStatus newStatus;
    private String changedBy;
    private String reason;
    private Instant timestamp;
}
