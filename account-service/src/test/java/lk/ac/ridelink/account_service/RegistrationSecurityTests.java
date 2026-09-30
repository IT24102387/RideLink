package lk.ac.ridelink.account_service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import lk.ac.ridelink.account_service.model.User;
import lk.ac.ridelink.account_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// A real servlet container is needed: MockMvc does not automatically dispatch failures to /error.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationSecurityTests {
    @Value("${local.server.port}")
    private int port;

    @MockitoBean
    private UserRepository repository;

    private HttpResponse<String> postRegistration() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"firstName":"Nimal","lastName":"Perera","email":"nimal@example.com",
                         "password":"RideLink123!","phoneNumber":"0771234567","role":"PASSENGER"}
                        """))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void anonymousRegistrationSucceedsWithoutCreatingSession() throws Exception {
        when(repository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("registered-user");
            return user;
        });
        var response = postRegistration();
        assertEquals(201, response.statusCode());
        assertTrue(response.headers().allValues("set-cookie").isEmpty());
        assertFalse(response.body().contains("password"));
    }

    @Test
    void registrationFailureIsNotMaskedAsUnauthorized() throws Exception {
        when(repository.existsByEmail(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Simulated database failure"));
        var response = postRegistration();
        assertEquals(500, response.statusCode());
        assertFalse(response.body().contains("Simulated database failure"));
        assertFalse(response.body().contains("trace"));
    }

    @Test
    void directRequestsToOtherEndpointsRemainProtectedAndStateless() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (String path : new String[] {"/api/users", "/api/auth/register", "/error"}) {
                var response = client.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path)).GET().build(),
                        HttpResponse.BodyHandlers.ofString());
                assertEquals(401, response.statusCode(), path);
                assertTrue(response.headers().allValues("set-cookie").isEmpty(), path);
            }
        }
    }
}
