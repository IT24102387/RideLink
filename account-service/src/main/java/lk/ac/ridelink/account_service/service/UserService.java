package lk.ac.ridelink.account_service.service;

import java.util.Locale;
import lk.ac.ridelink.account_service.dto.RegisterRequest;
import lk.ac.ridelink.account_service.dto.UserResponse;
import lk.ac.ridelink.account_service.dto.UpdateProfileRequest;
import lk.ac.ridelink.account_service.dto.UpdateRoleRequest;
import lk.ac.ridelink.account_service.dto.UpdateStatusRequest;
import lk.ac.ridelink.account_service.exception.EmailAlreadyExistsException;
import lk.ac.ridelink.account_service.exception.UserNotFoundException;
import lk.ac.ridelink.account_service.model.AccountStatus;
import lk.ac.ridelink.account_service.model.Role;
import lk.ac.ridelink.account_service.model.User;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getProfile(String userId) {
        return toResponse(userRepository.findById(userId).orElseThrow(UserNotFoundException::new));
    }

    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        return toResponse(userRepository.save(user));
    }

    public UserResponse updateRole(String userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setRole(Role.valueOf(request.role()));
        return toResponse(userRepository.save(user));
    }

    public UserResponse updateStatus(String userId, UpdateStatusRequest request) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setStatus(AccountStatus.valueOf(request.status()));
        return toResponse(userRepository.save(user));
    }

    public UserResponse register(RegisterRequest request) {
        String email = request.getEmail().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        User user = new User();
        user.setFirstName(request.getFirstName().strip());
        user.setLastName(request.getLastName().strip());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber().strip());
        user.setRole(Role.valueOf(request.getRole()));
        user.setStatus(AccountStatus.ACTIVE);

        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DuplicateKeyException exception) {
            // The unique email index also protects against concurrent registrations.
            throw new EmailAlreadyExistsException();
        }
        return toResponse(saved);
    }

    private UserResponse toResponse(User saved) {
        return new UserResponse(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getEmail(), saved.getPhoneNumber(), saved.getRole(), saved.getStatus(),
                saved.getCreatedAt(), saved.getUpdatedAt());
    }
}
