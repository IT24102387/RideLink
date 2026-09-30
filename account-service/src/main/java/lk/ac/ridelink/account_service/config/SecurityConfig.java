package lk.ac.ridelink.account_service.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import lk.ac.ridelink.account_service.model.AccountStatus;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository repository) throws Exception {
        var registration = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/register");
        var login = PathPatternRequestMatcher.withDefaults()
                .matcher(HttpMethod.POST, "/api/auth/login");
        return http
                .authorizeHttpRequests(auth -> auth
                        // Let the container render errors without replacing them with a 401.
                        // Direct client requests to /error still require authentication.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(registration, login).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                // All authentication is via an explicit Bearer header, never cookies.
                .csrf(csrf -> csrf.disable())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(token -> {
                    var user = repository.findById(token.getSubject())
                            .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                            .filter(account -> account.getRole() != null
                                    && account.getRole().name().equals(token.getClaimAsString("role")))
                            .orElseThrow(() -> new OAuth2AuthenticationException("invalid_token"));
                    return new JwtAuthenticationToken(token,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
                })))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .build();
    }
}
