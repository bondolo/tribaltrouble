package com.oddlabs.graphservlet;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * HTTP controller rendering telemetry charts for completed games.
 */
@Controller("/graphservlet")
public final class GraphController {
    private final DataSource dataSource;
    private final GraphRenderer graphRenderer;

    GraphController(DataSource dataSource, GraphRenderer graphRenderer) {
        this.dataSource = dataSource;
        this.graphRenderer = graphRenderer;
    }

    @Get(value = "/graph", produces = "image/png")
    HttpResponse<byte[]> graph(@QueryValue("game_id") int gameId) {
        int[][] data = getGameData(gameId);
        byte[] imageBytes = graphRenderer.renderPng(data);
        return HttpResponse.ok(imageBytes).contentType(MediaType.IMAGE_PNG);
    }

    private int[][] getGameData(int gameId) {
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT tick, team1, team2, team3, team4, team5, team6 FROM game_reports WHERE game_id = ? ORDER BY tick ASC")) {
            stmt.setInt(1, gameId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<int[]> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new int[]{
                            rs.getInt("tick"),
                            rs.getInt("team1"),
                            rs.getInt("team2"),
                            rs.getInt("team3"),
                            rs.getInt("team4"),
                            rs.getInt("team5"),
                            rs.getInt("team6")
                    });
                }
                return rows.toArray(new int[0][]);
            }
        } catch (SQLException e) {
            return new int[0][0];
        }
    }
}
