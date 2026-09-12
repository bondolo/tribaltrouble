package com.oddlabs.matchservlet;

import com.oddlabs.util.CryptUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;

/**
 * Controller handling user authentication and registration requests for matchmaking.
 */
@Controller("/matchservlet")
public final class LoginController {
    private static final String ERR_NO_SUCH_USER = "USER_ERROR_NO_SUCH_USER";
    private static final String ERR_TOO_SHORT = "USERNAME_ERROR_TOO_SHORT";
    private static final String ERR_TOO_LONG = "USERNAME_ERROR_TOO_LONG";
    private static final String ERR_INVALID_CHARS = "USERNAME_ERROR_INVALID_CHARACTERS";
    private static final String ERR_INVALID_EMAIL = "USER_ERROR_INVALID_EMAIL";
    private static final String ERR_ALREADY_EXISTS = "USERNAME_ERROR_ALREADY_EXISTS";

    private final DataSource dataSource;
    private final AuthTokenSigner tokenSigner;

    LoginController(DataSource dataSource, AuthTokenSigner tokenSigner) {
        this.dataSource = dataSource;
        this.tokenSigner = tokenSigner;
    }

    @Get(value = "/login", produces = MediaType.APPLICATION_OCTET_STREAM)
    HttpResponse<?> login(
            @QueryValue("username") String username,
            @QueryValue("password") String password,
            @Nullable @QueryValue("reg_key") String regKey) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT count(*) FROM registrations R WHERE lower(R.username) = lower(?) AND R.password = ? AND NOT R.disabled AND NOT R.banned")) {
            stmt.setString(1, username);
            stmt.setString(2, CryptUtils.digest(password));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next() || rs.getInt(1) == 0) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(ERR_NO_SUCH_USER);
                }
            }
        } catch (SQLException e) {
            return HttpResponse.serverError(e.getMessage());
        }

        byte[] token = tokenSigner.createSignedToken(username);
        return HttpResponse.ok(token).contentType(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Post(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED,
            produces = MediaType.APPLICATION_OCTET_STREAM)
    HttpResponse<?> register(
            HttpRequest<?> request,
            @Nullable @Body Map<String, String> body) {
        String username = getParam(request, body, "username");
        String password = getParam(request, body, "password");
        String email = getParam(request, body, "email");

        if (username == null || password == null || email == null) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(ERR_TOO_SHORT);
        }

        try (Connection conn = dataSource.getConnection()) {
            int minUsernameLength = getIntSetting(conn, "min_username_length", 2);
            int maxUsernameLength = getIntSetting(conn, "max_username_length", 20);
            String allowedChars = getSetting(conn, "allowed_chars", "");

            if (username.length() < minUsernameLength) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(ERR_TOO_SHORT);
            }
            if (username.length() > maxUsernameLength) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(ERR_TOO_LONG);
            }
            if (!UserValidation.checkChars(username, allowedChars)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(ERR_INVALID_CHARS);
            }
            if (!UserValidation.isValidEmail(email) || !UserValidation.checkChars(email, allowedChars)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body(ERR_INVALID_EMAIL);
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT count(*) FROM registrations WHERE lower(username) = lower(?)")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return HttpResponse.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.TEXT_PLAIN)
                                .body(ERR_ALREADY_EXISTS);
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO registrations (username, password, email) VALUES (?, ?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, CryptUtils.digest(password));
                stmt.setString(3, email);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            return HttpResponse.serverError(e.getMessage());
        }

        byte[] token = tokenSigner.createSignedToken(username);
        return HttpResponse.ok(token).contentType(MediaType.APPLICATION_OCTET_STREAM);
    }

    private static @Nullable String getParam(HttpRequest<?> request, @Nullable Map<String, String> body, String key) {
        if (body != null && body.containsKey(key)) {
            return body.get(key);
        }
        return request.getParameters().get(key);
    }

    private static String getSetting(Connection conn, String property, String defaultValue) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT value FROM settings WHERE property = ?")) {
            stmt.setString(1, property);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        return defaultValue;
    }

    private static int getIntSetting(Connection conn, String property, int defaultValue) throws SQLException {
        String val = getSetting(conn, property, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
