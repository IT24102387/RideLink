package lk.ac.ridelink.account_service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.controller.UserController;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.MediaType;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(ProfileTests.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "jwt.secret=test-only-secret-with-at-least-32-bytes")
class ProfileTests {
    private static final String UPDATE_BODY = """
            {"firstName":"  Sunil  ","lastName":"  Silva  ","phoneNumber":"  0779876543  "}
            """;
    @Configuration
    @Import({RegistrationTests.TestConfig.class, UserController.class})
    static class Config { }

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
        user.setPassword("secret-password-hash");
        user.setCreatedAt(LocalDateTime.of(2026, 9, 29, 12, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 9, 30, 12, 0));
        when(repository.findById(user.getId())).thenReturn(Optional.of(user));
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
    @EnumSource(Role.class)
    void returnsOnlySafeDatabaseProfileForAuthenticatedSubject(Role role) throws Exception {
        user.setRole(role);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token(false))
                        .param("id", "someone-else").param("email", "other@example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("current-user"))
                .andExpect(jsonPath("$.firstName").value("Nimal"))
                .andExpect(jsonPath("$.lastName").value("Perera"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository, times(2)).findById("current-user");
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "malformed", "expired", "tampered"})
    void rejectsMissingOrInvalidJwt(String scenario) throws Exception {
        for (var request : java.util.List.of(get("/api/users/me"),
                put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY),
                put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DRIVER\"}"))) {
        if (!"missing".equals(scenario)) {
            String jwt = token("expired".equals(scenario));
            if ("malformed".equals(scenario)) jwt = "not-a-jwt";
            if ("tampered".equals(scenario)) {
                int start = jwt.lastIndexOf('.') + 1;
                jwt = jwt.substring(0, start) + (jwt.charAt(start) == 'A' ? "B" : "A")
                        + jwt.substring(start + 1);
            }
            request.header("Authorization", "Bearer " + jwt);
        }
        mvc.perform(request).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void deletedUserInvalidatesAuthentication() throws Exception {
        when(repository.findById(user.getId())).thenReturn(Optional.empty());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isUnauthorized());
        verify(repository, never()).save(any());
    }

    @Test
    void userDeletedAfterAuthenticationReturns404() throws Exception {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user), Optional.empty());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "DISABLED"})
    void inactiveUserCannotViewProfile(AccountStatus status) throws Exception {
        user.setStatus(status);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isUnauthorized());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void updatesOnlyEditableFieldsAndReturnsSavedSafeProfile(Role role) throws Exception {
        user.setRole(role);
        LocalDateTime createdAt = user.getCreatedAt();
        LocalDateTime updatedAt = user.getUpdatedAt().plusHours(1);
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertEquals("current-user", saved.getId());
            assertEquals("nimal@example.com", saved.getEmail());
            assertEquals("secret-password-hash", saved.getPassword());
            assertEquals(role, saved.getRole());
            assertEquals(AccountStatus.ACTIVE, saved.getStatus());
            assertEquals(createdAt, saved.getCreatedAt());
            assertEquals("Sunil", saved.getFirstName());
            assertEquals("Silva", saved.getLastName());
            assertEquals("0779876543", saved.getPhoneNumber());
            // Simulate the existing MongoDB auditing callback on save.
            saved.setUpdatedAt(updatedAt);
            return saved;
        });
        String body = UPDATE_BODY.strip().replace("}", """
                ,"id":"other-user","email":"other@example.com","password":"hacked",
                "passwordHash":"hacked","role":"ADMIN","status":"DISABLED",
                "createdAt":"2000-01-01T00:00:00","updatedAt":"2000-01-01T00:00:00"}
                """);
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body)
                        .param("id", "other-user").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("current-user"))
                .andExpect(jsonPath("$.firstName").value("Sunil"))
                .andExpect(jsonPath("$.lastName").value("Silva"))
                .andExpect(jsonPath("$.phoneNumber").value("0779876543"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository, times(2)).findById("current-user");
        verify(repository).save(user);
        verifyNoMoreInteractions(repository);
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Sunil"));
    }

    static Stream<String> invalidUpdates() {
        return Stream.of("", "null", "{}", "{invalid", "[]",
                UPDATE_BODY.replace("\"  Sunil  \"", "null"),
                UPDATE_BODY.replace("  Sunil  ", " "),
                UPDATE_BODY.replace("  Silva  ", " "),
                UPDATE_BODY.replace("  0779876543  ", " "),
                UPDATE_BODY.replace("  Sunil  ", "a".repeat(101)),
                UPDATE_BODY.replace("  Silva  ", "a".repeat(101)),
                UPDATE_BODY.replace("  0779876543  ", "1".repeat(33)));
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void invalidUpdateReturns400WithoutSaving(String body) throws Exception {
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
        assertEquals("Nimal", user.getFirstName());
    }

    @Test
    void updateReturns404IfUserDeletedAfterAuthentication() throws Exception {
        when(repository.findById(user.getId())).thenReturn(Optional.of(user)).thenReturn(Optional.empty());
        mvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASSENGER", "DRIVER"})
    void changesRolePreservesProfileAndRequiresLoginWithNewRole(Role target) throws Exception {
        user.setRole(target == Role.DRIVER ? Role.PASSENGER : Role.DRIVER);
        String oldToken = token(false);
        user.setPassword(passwords.encode("RideLink123!"));
        String hash = user.getPassword();
        var createdAt = user.getCreatedAt();
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertEquals("current-user", saved.getId());
            assertEquals("Nimal", saved.getFirstName());
            assertEquals("Perera", saved.getLastName());
            assertEquals("nimal@example.com", saved.getEmail());
            assertEquals("0771234567", saved.getPhoneNumber());
            assertEquals(hash, saved.getPassword());
            assertEquals(AccountStatus.ACTIVE, saved.getStatus());
            assertEquals(createdAt, saved.getCreatedAt());
            assertEquals(target, saved.getRole());
            return saved;
        });
        String body = """
                {"role":"%s","id":"other-user","firstName":"Hacked","lastName":"Hacked",
                "email":"other@example.com","phoneNumber":"000","password":"hacked",
                "passwordHash":"hacked","status":"DISABLED","createdAt":"2000-01-01T00:00:00"}
                """.formatted(target.name());
        mvc.perform(put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .param("id", "other-user").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("current-user"))
                .andExpect(jsonPath("$.firstName").value("Nimal"))
                .andExpect(jsonPath("$.lastName").value("Perera"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value(target.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository).save(user);
        verify(repository, never()).findById("other-user");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        String loginBody = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimal@example.com\",\"password\":\"RideLink123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newToken = JsonMapper.builder().build().readTree(loginBody).get("token").asText();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(target.name()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{invalid", "{\"role\":null}", "{\"role\":\"\"}",
            "{\"role\":\" \"}", "{\"role\":\"ADMIN\"}", "{\"role\":\"driver\"}",
            "{\"role\":0}", "{\"role\":true}", "{\"role\":[]}"})
    void rejectsInvalidRoleWithoutSaving(String body) throws Exception {
        mvc.perform(put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("Authorization", "Bearer " + token(false)))
                .andExpect(status().isBadRequest());
        verify(repository, never()).save(any());
        assertEquals(Role.PASSENGER, user.getRole());
    }

    @Test
    void sameRoleKeepsTokenUsable() throws Exception {
        String jwt = token(false);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        mvc.perform(put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"PASSENGER\"}").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"deleted", "deletedAfterAuthentication", "SUSPENDED", "DISABLED"})
    void rejectsRoleChangesForUnavailableUsers(String scenario) throws Exception {
        if ("deleted".equals(scenario)) {
            when(repository.findById(user.getId())).thenReturn(Optional.empty());
        } else if ("deletedAfterAuthentication".equals(scenario)) {
            when(repository.findById(user.getId())).thenReturn(Optional.of(user)).thenReturn(Optional.empty());
        } else {
            user.setStatus(AccountStatus.valueOf(scenario));
        }
        mvc.perform(put("/api/users/me/role").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DRIVER\"}").header("Authorization", "Bearer " + token(false)))
                .andExpect(status().is("deletedAfterAuthentication".equals(scenario) ? 404 : 401));
        verify(repository, never()).save(any());
    }
}
