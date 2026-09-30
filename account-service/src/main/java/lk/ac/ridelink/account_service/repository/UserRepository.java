package lk.ac.ridelink.account_service.repository;

import java.util.Optional;

import lk.ac.ridelink.account_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
