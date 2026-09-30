package lk.ac.ridelink.account_service;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import lk.ac.ridelink.account_service.config.PasswordConfig;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.service.LoginService;
import org.springframework.test.context.TestPropertySource;
import lk.ac.ridelink.account_service.config.SecurityConfig;
import lk.ac.ridelink.account_service.controller.AuthController;
import lk.ac.ridelink.account_service.exception.GlobalExceptionHandler;
import lk.ac.ridelink.account_service.model.AccountStatus;
import lk.ac.ridelink.account_service.model.User;
import lk.ac.ridelink.account_service.repository.UserRepository;
import lk.ac.ridelink.account_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(RegistrationTests.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = "jwt.secret=test-only-secret-with-at-least-32-bytes")
class RegistrationTests {
    private static final String BODY = """
            {"firstName":"Nimal","lastName":"Perera","email":"  NIMAL@Example.com  ",
             "password":"RideLink123!","phoneNumber":"0771234567","role":"PASSENGER"}
            """;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({AuthController.class, UserService.class, PasswordConfig.class,
            SecurityConfig.class, GlobalExceptionHandler.class, JwtConfig.class, LoginService.class})
    static class TestConfig {
        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository repository;
    @Autowired private PasswordEncoder encoder;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(repository);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PASSENGER", "DRIVER"})
    void registersBothRolesWithEncodedPasswordAndSafeResponse(String role) throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 29, 12, 0);
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("nimal@example.com", user.getEmail());
            assertEquals(AccountStatus.ACTIVE, user.getStatus());
            assertNotEquals("RideLink123!", user.getPassword());
            assertTrue(encoder.matches("RideLink123!", user.getPassword()));
            assertNull(user.getId());
            user.setId("saved-id");
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            return user;
        });

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.replace("PASSENGER", role)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("saved-id"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(repository).existsByEmail("nimal@example.com");
    }

    @Test
    void rejectsExistingEmailBeforeSaving() throws Exception {
        when(repository.existsByEmail("nimal@example.com")).thenReturn(true);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.password").doesNotExist());
        verify(repository, never()).save(any());
    }

    @Test
    void handlesConcurrentDuplicateWithoutLeakingDatabaseDetails() throws Exception {
        when(repository.save(any())).thenThrow(new DuplicateKeyException("sensitive database details"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    static Stream<String> invalidRequests() {
        return Stream.of("{}", "{invalid", BODY.replace("Nimal", " "),
                BODY.replace("Perera", ""), BODY.replace("  NIMAL@Example.com  ", "not-an-email"),
                BODY.replace("RideLink123!", "short"), BODY.replace("RideLink123!", "        "),
                BODY.replace("0771234567", ""), BODY.replace("\"PASSENGER\"", "null"),
                BODY.replace("PASSENGER", "ADMIN"), BODY.replace("\"PASSENGER\"", "0"),
                BODY.replace("RideLink123!", "a".repeat(73)),
                BODY.replace("RideLink123!", "é".repeat(37)));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsWithoutTouchingRepository(String body) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.errors").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(repository);
    }

    @Test
    void protectsOtherPathsAndMethods() throws Exception {
        mvc.perform(get("/api/auth/register")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/users")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
}
