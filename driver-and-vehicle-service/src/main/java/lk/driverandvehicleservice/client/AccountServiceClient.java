package lk.driverandvehicleservice.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class AccountServiceClient {

    private final WebClient accountServiceWebClient;

    public AccountServiceClient(@Qualifier("accountServiceWebClient") WebClient accountServiceWebClient) {
        this.accountServiceWebClient = accountServiceWebClient;
    }

    public Optional<AccountDto> getAccountById(String userId) {
        try {
            AccountDto account = accountServiceWebClient.get()
                    .uri("/accounts/{id}", userId)
                    .retrieve()
                    .bodyToMono(AccountDto.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();
            return Optional.ofNullable(account);
        } catch (WebClientResponseException.NotFound ex) {
            log.warn("Account not found in Account Service for userId: {}", userId);
            return Optional.empty();
        } catch (Exception ex) {
            log.error("Failed to query Account Service for userId {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isUserValidDriver(String userId) {
        return getAccountById(userId)
                .map(acc -> "DRIVER".equalsIgnoreCase(acc.getRole()) || "ROLE_DRIVER".equalsIgnoreCase(acc.getRole()))
                .orElse(false);
    }

    @Data
    public static class AccountDto {
        private String id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String status;
    }
}
