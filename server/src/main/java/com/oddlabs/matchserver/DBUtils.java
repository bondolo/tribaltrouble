package com.oddlabs.matchserver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.sqlite.SQLiteDataSource;

/**
 * Database access utilities and connection management.
 */
@NullMarked
public final class DBUtils {
    public static final String DEFAULT_SQLITE_URL = "jdbc:sqlite:file:oddlabs?mode=memory&cache=shared";

    private static final Logger logger = Logger.getLogger(DBUtils.class.getName());
    private static @Nullable DataSource dataSource;
    private static @Nullable Connection keepAliveConnection;

    private DBUtils() {
    }

    public static synchronized void initDataSource(DataSource ds) {
        dataSource = ds;
    }

    public static synchronized void initConnection(String address, @Nullable String user, @Nullable String password) {
        String dbUrl = System.getProperty("tribaltrouble.db.url", address);
        if (dbUrl.isEmpty() || dbUrl.contains(":memory:") || "memory".equalsIgnoreCase(System.getProperty(
                "tribaltrouble.db.type"))) {
            initInMemoryDatabase();
            return;
        }
        try {
            SQLiteDataSource sqliteDs = new SQLiteDataSource();
            sqliteDs.setUrl(dbUrl);
            try (Connection c = sqliteDs.getConnection()) {
                // Verify connection
            }
            dataSource = sqliteDs;
            createDefaultSchema(sqliteDs);
        } catch (Exception e) {
            initInMemoryDatabase();
        }
    }

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            initInMemoryDatabase();
        }
        return Objects.requireNonNull(dataSource);
    }

    public static synchronized void initInMemoryDatabase() {
        if (keepAliveConnection != null) {
            try {
                keepAliveConnection.close();
            } catch (SQLException e) {
                logger.finer(() -> "Previous keep-alive connection close exception: " + e);
            }
            keepAliveConnection = null;
        }
        SQLiteDataSource sqliteDs = new SQLiteDataSource();
        sqliteDs.setUrl(DEFAULT_SQLITE_URL);
        try {
            // Keep a persistent connection open so SQLite doesn't discard shared in-memory state
            keepAliveConnection = sqliteDs.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open SQLite in-memory keep-alive connection", e);
        }
        dataSource = sqliteDs;
        createDefaultSchema(sqliteDs);
    }

    private static void createDefaultSchema(DataSource ds) {
        Flyway flyway = Flyway.configure(DBUtils.class.getClassLoader())
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    public static Connection createDatabaseConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static PreparedStatement createStatement(String sql) throws SQLException {
        return createDatabaseConnection().prepareStatement(sql);
    }

    public static void postHermesMessage(String message) throws SQLException {
        try (Connection conn = createDatabaseConnection(); PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO messages (time, message) VALUES (CURRENT_TIMESTAMP, ?)")) {
            stmt.setString(1, message);
            stmt.executeUpdate();
        }
    }
}
