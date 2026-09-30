package lk.ac.ridelink.account_service;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.model.*;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(LoginSecurityTests.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "jwt.secret=test-only-secret-with-at-least-32-bytes")
class LoginSecurityTests {
    private static final String BODY = """
            {"email":"  NIMAL@Example.com  ","password":"RideLink123!"}
            """;

    @Configuration
    @Import({RegistrationTests.TestConfig.class, PrivateController.class})
    static class Config { }

    // A test-only endpoint proves authentication and role mapping without adding a production API.
    @RestController
    static class PrivateController {
        @RequestMapping(value = "/api/private", method = {RequestMethod.GET, RequestMethod.POST})
        Map<String, String> privateEndpoint(Authentication authentication) {
            return Map.of("id", authentication.getName(), "role",
                    authentication.getAuthorities().iterator().next().getAuthority());
        }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository repository;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        reset(repository);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        user = new User();
        user.setId("user-123");
        user.setRole(Role.DRIVER);
        user.setPassword(passwords.encode("RideLink123!"));
        when(repository.findByEmail("nimal@example.com")).thenReturn(Optional.of(user));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(body).get("token").asText();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void loginSignsClaimsAndAuthenticatesPrivateGetAndPost(Role role) throws Exception {
        user.setRole(role);
        String token = login();
        Jwt jwt = decoder.decode(token);
        assertEquals("user-123", jwt.getSubject());
        assertEquals(role.name(), jwt.getClaimAsString("role"));
        assertEquals(3600, java.time.Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds());
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertFalse(jwt.getClaims().containsKey("password"));
        for (var request : java.util.List.of(get("/api/private"), post("/api/private"))) {
            mvc.perform(request.header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("user-123"))
                    .andExpect(jsonPath("$.role").value("ROLE_" + role.name()))
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
        }
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
        verify(repository).findByEmail("nimal@example.com");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong", "unknown", "overlong"})
    void invalidCredentialsReturnGeneric401(String scenario) throws Exception {
        String body = switch (scenario) {
            case "unknown" -> BODY.replace("NIMAL", "UNKNOWN");
            case "overlong" -> BODY.replace("RideLink123!", "a".repeat(73));
            default -> BODY.replace("RideLink123!", "wrong-password");
        };
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "DISABLED"})
    void blockedAccountsCannotLoginOrUseExistingToken(AccountStatus status) throws Exception {
        String token = login();
        user.setStatus(status);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist());
        mvc.perform(get("/api/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{invalid", "{\"email\":\"invalid\",\"password\":\"x\"}",
            "{\"email\":\"a@example.com\",\"password\":\" \"}"})
    void invalidLoginPayloadIs400(String body) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    private String signedToken(String scenario) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("issuer".equals(scenario) ? "other" : JwtConfig.ISSUER)
                .issuedAt(now.minusSeconds(120));
        if (!"subject".equals(scenario)) claims.subject("user-123");
        if (!"role".equals(scenario)) claims.claim("role", "DRIVER");
        if (!"expiration".equals(scenario)) claims.expiresAt(
                "expired".equals(scenario) ? now.minusSeconds(1) : now.plusSeconds(3600));
        if ("future".equals(scenario)) claims.notBefore(now.plusSeconds(3600));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims.build())).getTokenValue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "issuer", "subject", "role", "expiration", "future", "tampered", "malformed"})
    void rejectsInvalidTokensBeforeDatabaseLookup(String scenario) throws Exception {
        String token = signedToken(scenario);
        if ("tampered".equals(scenario)) {
            int signature = token.lastIndexOf('.') + 1;
            token = token.substring(0, signature) + (token.charAt(signature) == 'A' ? "B" : "A")
                    + token.substring(signature + 1);
        }
        if ("malformed".equals(scenario)) token = "not-a-jwt";
        mvc.perform(get("/api/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void deletedAccountsAndChangedRolesInvalidateTokens() throws Exception {
        String token = login();
        user.setRole(Role.PASSENGER);
        mvc.perform(get("/api/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        when(repository.findById(user.getId())).thenReturn(Optional.empty());
        mvc.perform(get("/api/private").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
