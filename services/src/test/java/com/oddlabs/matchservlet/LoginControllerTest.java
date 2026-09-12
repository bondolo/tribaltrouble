package com.oddlabs.matchservlet;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Flyway migrations, account registration, and authentication endpoints.
 */
@MicronautTest
final class LoginControllerTest {
    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private DataSource dataSource;

    @Inject
    private AuthTokenSigner tokenSigner;

    private BlockingHttpClient client;

    @BeforeEach
    void setUp() {
        client = httpClient.toBlocking();
    }

    private void assertValidToken(byte[] responseBytes, String expectedUsername) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(responseBytes));
        long timestamp = in.readLong();
        int sigLength = in.readInt();
        byte[] sig = in.readNBytes(sigLength);
        assertTrue(timestamp > 0);
        assertTrue(sigLength > 0);
        assertTrue(tokenSigner.verifyToken(expectedUsername, timestamp, sig));
    }

    @Test
    void flywayMigrationPopulatesSchemaAndSettings() throws SQLException {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT value FROM settings WHERE property = ?")) {
            stmt.setString(1, "revision");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("1116", rs.getString("value"));
            }
            stmt.setString(1, "max_profiles");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("5", rs.getString("value"));
            }
        }
    }

    @Test
    void accountCreationAndLoginSuccess() throws Exception {
        Map<String, String> form = Map.of(
                "username", "testuser",
                "password", "secretpass",
                "email", "test@oddlabs.com"
        );
        HttpRequest<?> registerReq = HttpRequest.POST("/matchservlet/login", form)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpResponse<byte[]> registerRes = client.exchange(registerReq, byte[].class);
        assertEquals(HttpStatus.OK, registerRes.getStatus());
        assertNotNull(registerRes.body());
        assertValidToken(registerRes.body(), "testuser");

        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT email FROM registrations WHERE username = ?")) {
            stmt.setString(1, "testuser");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("test@oddlabs.com", rs.getString("email"));
            }
        }

        HttpRequest<?> loginReq = HttpRequest.GET("/matchservlet/login?username=testuser&password=secretpass");
        HttpResponse<byte[]> loginRes = client.exchange(loginReq, byte[].class);
        assertEquals(HttpStatus.OK, loginRes.getStatus());
        assertNotNull(loginRes.body());
        assertValidToken(loginRes.body(), "testuser");
    }

    @Test
    void loginInvalidPasswordReturns403() {
        Map<String, String> form = Map.of(
                "username", "validuser",
                "password", "correctpass",
                "email", "valid@oddlabs.com"
        );
        client.exchange(HttpRequest.POST("/matchservlet/login", form)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED), byte[].class);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.GET("/matchservlet/login?username=validuser&password=wrongpass"), String.class)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("USER_ERROR_NO_SUCH_USER", ex.getResponse().getBody(String.class).orElse(""));
    }

    @Test
    void duplicateRegistrationReturns403() {
        Map<String, String> form1 = Map.of(
                "username", "dupuser",
                "password", "pass123",
                "email", "dup@oddlabs.com"
        );
        client.exchange(HttpRequest.POST("/matchservlet/login", form1)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED), byte[].class);

        Map<String, String> form2 = Map.of(
                "username", "dupuser",
                "password", "otherpass",
                "email", "dup2@oddlabs.com"
        );
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.POST("/matchservlet/login", form2)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("USERNAME_ERROR_ALREADY_EXISTS", ex.getResponse().getBody(String.class).orElse(""));
    }

    @Test
    void validationFailuresReturnAppropriateError() {
        Map<String, String> tooShort = Map.of(
                "username", "a",
                "password", "pass123",
                "email", "short@oddlabs.com"
        );
        HttpClientResponseException ex1 = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.POST("/matchservlet/login", tooShort)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class)
        );
        assertEquals("USERNAME_ERROR_TOO_SHORT", ex1.getResponse().getBody(String.class).orElse(""));

        Map<String, String> tooLong = Map.of(
                "username", "verylongusernamethatexceedstwenty",
                "password", "pass123",
                "email", "long@oddlabs.com"
        );
        HttpClientResponseException ex2 = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.POST("/matchservlet/login", tooLong)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class)
        );
        assertEquals("USERNAME_ERROR_TOO_LONG", ex2.getResponse().getBody(String.class).orElse(""));

        Map<String, String> invalidChars = Map.of(
                "username", "user with space",
                "password", "pass123",
                "email", "space@oddlabs.com"
        );
        HttpClientResponseException ex3 = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.POST("/matchservlet/login", invalidChars)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class)
        );
        assertEquals("USERNAME_ERROR_INVALID_CHARACTERS", ex3.getResponse().getBody(String.class).orElse(""));

        Map<String, String> invalidEmail = Map.of(
                "username", "bademailuser",
                "password", "pass123",
                "email", "notanemail"
        );
        HttpClientResponseException ex4 = assertThrows(HttpClientResponseException.class, () -> client.exchange(
                HttpRequest.POST("/matchservlet/login", invalidEmail)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED), String.class)
        );
        assertEquals("USER_ERROR_INVALID_EMAIL", ex4.getResponse().getBody(String.class).orElse(""));
    }
}
