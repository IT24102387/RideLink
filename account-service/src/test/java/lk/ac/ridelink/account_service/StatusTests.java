package lk.ac.ridelink.account_service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.model.*;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ProfileTests.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "jwt.secret=test-only-secret-with-at-least-32-bytes")
class StatusTests {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository repository;
    @Autowired JwtEncoder encoder;
    @Autowired PasswordEncoder passwords;
    private MockMvc mvc;
    private User user;

    @BeforeEach
    void setUp() {
        reset(repository);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        user = new User();
        user.setId("current-user");
        user.setFirstName("Nimal");
        user.setLastName("Perera");
        user.setEmail("nimal@example.com");
        user.setPhoneNumber("0771234567");
        user.setRole(Role.PASSENGER);
        user.setPassword(passwords.encode("RideLink123!"));
        user.setCreatedAt(LocalDateTime.of(2026, 9, 29, 12, 0));
        user.setUpdatedAt(user.getCreatedAt());
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    private String token(boolean expired) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(JwtConfig.ISSUER).subject(user.getId())
                .claim("role", user.getRole().name()).issuedAt(now.minusSeconds(120))
                .expiresAt(expired ? now.minusSeconds(1) : now.plusSeconds(3600)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @ParameterizedTest
    @EnumSource(AccountStatus.class)
    void updatesOnlyStatusAndEnforcesAccessOnFollowingRequests(AccountStatus target) throws Exception {
        String jwt = token(false);
        String hash = user.getPassword();
        var created = user.getCreatedAt();
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertEquals("current-user", saved.getId());
            assertEquals("Nimal", saved.getFirstName());
            assertEquals("Perera", saved.getLastName());
            assertEquals("nimal@example.com", saved.getEmail());
            assertEquals("0771234567", saved.getPhoneNumber());
            assertEquals(Role.PASSENGER, saved.getRole());
            assertEquals(hash, saved.getPassword());
            assertEquals(created, saved.getCreatedAt());
            assertEquals(target, saved.getStatus());
            // Simulate the existing MongoDB auditing callback.
            saved.setUpdatedAt(created.plusDays(1));
            return saved;
        });
        String body = """
                {"status":"%s","id":"other-user","firstName":"Hacked","lastName":"Hacked",
                "email":"other@example.com","phoneNumber":"000","role":"DRIVER",
                "password":"hacked","passwordHash":"hacked","createdAt":"2000-01-01T00:00:00"}
                """.formatted(target.name());
        mvc.perform(put("/api/users/me/status").contentType(MediaType.APPLICATION_JSON).content(body)
                        .param("id", "other-user").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("current-user"))
                .andExpect(jsonPath("$.firstName").value("Nimal"))
                .andExpect(jsonPath("$.lastName").value("Perera"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.status").value(target.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository).save(user);
        verify(repository, never()).findById("other-user");

        int expected = target == AccountStatus.ACTIVE ? 200 : 401;
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().is(expected));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimal@example.com\",\"password\":\"RideLink123!\"}"))
                .andExpect(status().is(expected));
        if (target != AccountStatus.ACTIVE) {
            for (var request : List.of(
                    put("/api/users/me").content("{\"firstName\":\"Sunil\",\"lastName\":\"Silva\",\"phoneNumber\":\"0771234567\"}"),
                    put("/api/users/me/role").content("{\"role\":\"DRIVER\"}"),
                    put("/api/users/me/status").content("{\"status\":\"ACTIVE\"}"))) {
                mvc.perform(request.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + jwt))
                        .andExpect(status().isUnauthorized());
            }
            verify(repository, times(1)).save(any());
            assertEquals(target, user.getStatus());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "null", "{invalid", "{\"status\":null}", "{\"status\":\"\"}",
            "{\"status\":\" \"}", "{\"status\":\"UNKNOWN\"}", "{\"status\":\"active\"}",
            "{\"status\":0}", "{\"status\":true}", "{\"status\":[]}"})
    void invalidOrMissingStatusReturns400(String body) throws Exception {
        mvc.perform(put("/api/users/me/status").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
        assertEquals(AccountStatus.ACTIVE, user.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "malformed", "expired", "tampered"})
    void rejectsMissingOrInvalidJwt(String scenario) throws Exception {
        var request = put("/api/users/me/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\"}");
        if (!"missing".equals(scenario)) {
            String jwt = token("expired".equals(scenario));
            if ("malformed".equals(scenario)) jwt = "not-a-jwt";
            if ("tampered".equals(scenario)) {
                int start = jwt.lastIndexOf('.') + 1;
                jwt = jwt.substring(0, start) + (jwt.charAt(start) == 'A' ? "B" : "A") + jwt.substring(start + 1);
            }
            request.header("Authorization", "Bearer " + jwt);
        }
        mvc.perform(request).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"deleted", "deletedAfterAuthentication", "SUSPENDED", "DISABLED"})
    void rejectsUnavailableUser(String scenario) throws Exception {
        if ("deleted".equals(scenario)) {
            when(repository.findById(user.getId())).thenReturn(Optional.empty());
        } else if ("deletedAfterAuthentication".equals(scenario)) {
            when(repository.findById(user.getId())).thenReturn(Optional.of(user)).thenReturn(Optional.empty());
        } else {
            user.setStatus(AccountStatus.valueOf(scenario));
        }
        mvc.perform(put("/api/users/me/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().is("deletedAfterAuthentication".equals(scenario) ? 404 : 401));
        verify(repository, never()).save(any());
    }
}
