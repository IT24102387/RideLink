package lk.ridemanagementservice.repository;

import lk.ridemanagementservice.model.Ride;
import lk.ridemanagementservice.model.RideStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RideRepository extends MongoRepository<Ride, String> {

    List<Ride> findByPassengerIdOrderByCreatedAtDesc(String passengerId);

    List<Ride> findByDriverIdOrderByCreatedAtDesc(String driverId);

    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByPassengerIdAndStatus(String passengerId, RideStatus status);

    List<Ride> findByDriverIdAndStatus(String driverId, RideStatus status);
}
