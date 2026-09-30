package lk.ac.ridelink.account_service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lk.ac.ridelink.account_service.config.JwtConfig;
import lk.ac.ridelink.account_service.controller.AdminUserController;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AdminRoleTests.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = "jwt.secret=test-only-secret-with-at-least-32-bytes")
class AdminRoleTests {
    @Configuration
    @Import({ProfileTests.Config.class, AdminUserController.class})
    static class Config { }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository repository;
    @Autowired JwtEncoder encoder;
    @Autowired PasswordEncoder passwords;
    private MockMvc mvc;
    private User admin;
    private User target;

    @BeforeEach
    void setUp() {
        reset(repository);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        admin = new User();
        admin.setId("admin-id");
        admin.setRole(Role.ADMIN);
        target = new User();
        target.setId("target-id");
        target.setFirstName("Nimal");
        target.setLastName("Perera");
        target.setEmail("nimal@example.com");
        target.setPhoneNumber("0771234567");
        target.setRole(Role.PASSENGER);
        target.setPassword(passwords.encode("RideLink123!"));
        target.setCreatedAt(LocalDateTime.of(2026, 9, 29, 12, 0));
        target.setUpdatedAt(target.getCreatedAt());
        when(repository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(repository.findById(target.getId())).thenReturn(Optional.of(target));
        when(repository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));
    }

    private String token(User user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(JwtConfig.ISSUER).subject(user.getId())
                .claim("role", user.getRole().name()).issuedAt(now).expiresAt(now.plusSeconds(3600)).build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASSENGER", "DRIVER"})
    void adminChangesRoleAndTargetMustLoginAgain(Role newRole) throws Exception {
        target.setRole(newRole == Role.DRIVER ? Role.PASSENGER : Role.DRIVER);
        String oldToken = token(target);
        String password = target.getPassword();
        var createdAt = target.getCreatedAt();
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertSame(target, saved);
            assertEquals(newRole, saved.getRole());
            assertEquals(password, saved.getPassword());
            assertEquals(createdAt, saved.getCreatedAt());
            assertEquals(AccountStatus.ACTIVE, saved.getStatus());
            saved.setUpdatedAt(createdAt.plusDays(1));
            return saved;
        });
        mvc.perform(put("/api/admin/users/target-id/role").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + newRole.name() + "\",\"id\":\"admin-id\",\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("target-id"))
                .andExpect(jsonPath("$.firstName").value("Nimal"))
                .andExpect(jsonPath("$.lastName").value("Perera"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value(newRole.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        verify(repository).save(target);
        assertEquals(Role.ADMIN, admin.getRole());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimal@example.com\",\"password\":\"RideLink123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newToken = JsonMapper.builder().build().readTree(login).get("token").asText();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(newRole.name()));
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"PASSENGER", "DRIVER"})
    void nonAdminsReceive403WithoutTargetLookup(Role role) throws Exception {
        admin.setRole(role);
        mvc.perform(put("/api/admin/users/target-id/role").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DRIVER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/target-id/status").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isForbidden());
        verify(repository, never()).findById("target-id");
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "invalid", "tampered", "demoted", "suspended", "disabled", "deleted"})
    void rejectsInvalidAuthenticationAndStaleAdminAuthority(String scenario) throws Exception {
        String jwt = token(admin);
        switch (scenario) {
            case "invalid" -> jwt = "invalid-jwt";
            case "tampered" -> {
                int start = jwt.lastIndexOf('.') + 1;
                jwt = jwt.substring(0, start) + (jwt.charAt(start) == 'A' ? "B" : "A") + jwt.substring(start + 1);
            }
            case "demoted" -> admin.setRole(Role.DRIVER);
            case "suspended" -> admin.setStatus(AccountStatus.SUSPENDED);
            case "disabled" -> admin.setStatus(AccountStatus.DISABLED);
            case "deleted" -> when(repository.findById(admin.getId())).thenReturn(Optional.empty());
        }
        var request = put("/api/admin/users/target-id/role").contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"DRIVER\"}");
        if (!"missing".equals(scenario)) request.header("Authorization", "Bearer " + jwt);
        mvc.perform(request).andExpect(status().isUnauthorized());
        var statusRequest = put("/api/admin/users/target-id/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SUSPENDED\"}");
        if (!"missing".equals(scenario)) statusRequest.header("Authorization", "Bearer " + jwt);
        mvc.perform(statusRequest).andExpect(status().isUnauthorized());
        verify(repository, never()).findById("target-id");
        verify(repository, never()).save(any());
    }

    @Test
    void missingTargetReturns404() throws Exception {
        mvc.perform(put("/api/admin/users/unknown/role").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"DRIVER\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("User not found"));
        mvc.perform(put("/api/admin/users/unknown/status").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("User not found"));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "null", "{invalid", "{\"role\":null}", "{\"role\":\"\"}",
            "{\"role\":\"ADMIN\"}", "{\"role\":\"driver\"}", "{\"role\":0}", "{\"role\":[]}"})
    void rejectsInvalidRoleAndAdminPromotion(String body) throws Exception {
        mvc.perform(put("/api/admin/users/target-id/role").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(repository, never()).findById("target-id");
        verify(repository, never()).save(any());
    }

    @Test
    void publicRegistrationCannotCreateAdmin() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"firstName":"Nimal","lastName":"Perera","email":"nimal@example.com",
                "password":"RideLink123!","phoneNumber":"0771234567","role":"ADMIN"}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.role").exists());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "null", "{invalid", "{\"status\":null}", "{\"status\":\"\"}",
            "{\"status\":\"UNKNOWN\"}", "{\"status\":\"active\"}", "{\"status\":0}",
            "{\"status\":true}", "{\"status\":[]}"})
    void rejectsInvalidOrMissingStatus(String body) throws Exception {
        mvc.perform(put("/api/admin/users/target-id/status").header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(repository, never()).findById("target-id");
        verify(repository, never()).save(any());
        assertEquals(AccountStatus.ACTIVE, target.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "DISABLED"})
    void adminBlocksAndReactivatesTargetWithoutChangingOtherFields(AccountStatus blockedStatus) throws Exception {
        String adminToken = token(admin);
        String targetToken = token(target);
        String password = target.getPassword();
        var createdAt = target.getCreatedAt();
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            assertSame(target, saved);
            assertEquals("target-id", saved.getId());
            assertEquals("Nimal", saved.getFirstName());
            assertEquals("Perera", saved.getLastName());
            assertEquals("nimal@example.com", saved.getEmail());
            assertEquals("0771234567", saved.getPhoneNumber());
            assertEquals(Role.PASSENGER, saved.getRole());
            assertEquals(password, saved.getPassword());
            assertEquals(createdAt, saved.getCreatedAt());
            saved.setUpdatedAt(createdAt.plusDays(1));
            return saved;
        });
        changeStatusAndCheckProfile(blockedStatus, adminToken);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimal@example.com\",\"password\":\"RideLink123!\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.token").doesNotExist());
        for (var request : java.util.List.of(get("/api/users/me"),
                put("/api/users/me").content("{\"firstName\":\"Sunil\",\"lastName\":\"Silva\",\"phoneNumber\":\"0771234567\"}"),
                put("/api/users/me/role").content("{\"role\":\"DRIVER\"}"),
                put("/api/users/me/status").content("{\"status\":\"ACTIVE\"}"))) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).header("Authorization", "Bearer " + targetToken))
                    .andExpect(status().isUnauthorized());
        }
        assertEquals(blockedStatus, target.getStatus());
        changeStatusAndCheckProfile(AccountStatus.ACTIVE, adminToken);
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimal@example.com\",\"password\":\"RideLink123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String newToken = JsonMapper.builder().build().readTree(login).get("token").asText();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(repository, times(2)).save(target);
        assertEquals(AccountStatus.ACTIVE, admin.getStatus());
        assertEquals(Role.ADMIN, admin.getRole());
    }

    private void changeStatusAndCheckProfile(AccountStatus newStatus, String adminToken) throws Exception {
        String body = """
                {"status":"%s","id":"admin-id","firstName":"Hacked","lastName":"Hacked",
                "email":"other@example.com","phoneNumber":"000","role":"ADMIN",
                "password":"hacked","passwordHash":"hacked","createdAt":"2000-01-01T00:00:00",
                "updatedAt":"2000-01-01T00:00:00"}
                """.formatted(newStatus.name());
        mvc.perform(put("/api/admin/users/target-id/status").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$.id").value("target-id"))
                .andExpect(jsonPath("$.firstName").value("Nimal"))
                .andExpect(jsonPath("$.lastName").value("Perera"))
                .andExpect(jsonPath("$.email").value("nimal@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("0771234567"))
                .andExpect(jsonPath("$.role").value("PASSENGER"))
                .andExpect(jsonPath("$.status").value(newStatus.name()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
