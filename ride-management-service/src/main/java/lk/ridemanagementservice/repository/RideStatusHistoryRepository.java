package lk.ridemanagementservice.repository;

import lk.ridemanagementservice.model.RideStatusHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideStatusHistoryRepository extends MongoRepository<RideStatusHistory, String> {

    List<RideStatusHistory> findByRideIdOrderByTimestampAsc(String rideId);
}
