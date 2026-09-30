package lk.ac.ridelink.account_service.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTests {
    @ParameterizedTest
    @ValueSource(strings = {"", "short", "                               ", "                                "})
    void rejectsMissingWeakOrBlankSecrets(String secret) {
        assertThrows(IllegalArgumentException.class, () -> new JwtConfig().jwtKey(secret));
    }
}
