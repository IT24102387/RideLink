package lk.ac.ridelink.account_service.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt) {
    @Override
    public String toString() {
        return "LoginResponse[token redacted, tokenType=" + tokenType + ", expiresAt=" + expiresAt + "]";
    }
}
