package lk.ac.ridelink.account_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.dto.LoginRequest;
import lk.ac.ridelink.account_service.dto.LoginResponse;
import lk.ac.ridelink.account_service.model.AccountStatus;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final UserRepository repository;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final long expirationSeconds;
    private final String dummyHash;

    public LoginService(UserRepository repository, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${jwt.expiration-seconds:3600}") long expirationSeconds) {
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive");
        }
        this.repository = repository;
        this.passwords = passwords;
        this.encoder = encoder;
        this.expirationSeconds = expirationSeconds;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    public LoginResponse login(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Invalid email or password");
        }
        var user = repository.findByEmail(request.email()).orElse(null);
        boolean matches = passwords.matches(request.password(), user == null ? dummyHash : user.getPassword());
        if (!matches || user == null || user.getStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Invalid email or password");
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);
        var claims = JwtClaimsSet.builder().issuer(JwtConfig.ISSUER).subject(user.getId())
                .claim("role", user.getRole().name()).issuedAt(now).expiresAt(expiresAt).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResponse(token, "Bearer", expiresAt);
    }
}
