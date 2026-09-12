package com.oddlabs.graphservlet;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the telemetry chart PNG rendering endpoint.
 */
@MicronautTest
final class GraphControllerTest {
    @Inject
    @Client("/")
    private HttpClient httpClient;

    @Inject
    private DataSource dataSource;

    private BlockingHttpClient client;

    @BeforeEach
    void setUp() {
        client = httpClient.toBlocking();
    }

    @Test
    void graphEndpointReturnsValidPngChart() throws SQLException, IOException {
        int testGameId = 99;
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO game_reports (game_id, tick, team1, team2, team3, team4, team5, team6) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (int tick = 0; tick <= 30; tick += 5) {
                stmt.setInt(1, testGameId);
                stmt.setInt(2, tick);
                stmt.setInt(3, 100 + tick * 2);
                stmt.setInt(4, 90 + tick);
                stmt.setInt(5, 50);
                stmt.setInt(6, 40);
                stmt.setInt(7, 30);
                stmt.setInt(8, 20);
                stmt.executeUpdate();
            }
        }

        HttpRequest<?> req = HttpRequest.GET("/graphservlet/graph?game_id=" + testGameId);
        HttpResponse<byte[]> res = client.exchange(req, byte[].class);

        assertEquals(HttpStatus.OK, res.getStatus());
        assertTrue(res.getContentType().isPresent());
        assertEquals(MediaType.IMAGE_PNG_TYPE, res.getContentType().get());

        byte[] body = res.body();
        assertNotNull(body);
        assertTrue(body.length > 8);

        // Verify PNG magic bytes: 0x89 0x50 0x4E 0x47
        assertEquals((byte) 0x89, body[0]);
        assertEquals((byte) 0x50, body[1]);
        assertEquals((byte) 0x4E, body[2]);
        assertEquals((byte) 0x47, body[3]);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(body));
        assertNotNull(img);
        assertEquals(GraphRenderer.IMAGE_WIDTH, img.getWidth());
        assertEquals(GraphRenderer.IMAGE_HEIGHT, img.getHeight());
    }
}
