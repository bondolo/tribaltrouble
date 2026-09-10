package com.oddlabs.matchserver;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Database access utilities and connection management.
 */
@NullMarked
public final class DBUtils {
    private static @Nullable DataSource dataSource;
    private static final String DEFAULT_H2_URL
            = "jdbc:h2:mem:oddlabs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";

    private DBUtils() {
    }

    public static synchronized void initDataSource(DataSource ds) {
        dataSource = ds;
    }

    public static synchronized void initConnection(String address, String user, String password) {
        String dbUrl = System.getProperty("tribaltrouble.db.url", address);
        if (dbUrl.startsWith("jdbc:h2:") || "h2".equalsIgnoreCase(System.getProperty("tribaltrouble.db.type", "h2"))) {
            initInMemoryDatabase();
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection c = DriverManager.getConnection(dbUrl, user, password)) {
                // Connection successful
            }
            dataSource = new DriverManagerDataSource(dbUrl, user, password);
        } catch (Exception e) {
            // Fall back to in-memory H2 database for local development and testing
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
        JdbcDataSource h2Ds = new JdbcDataSource();
        h2Ds.setURL(DEFAULT_H2_URL);
        h2Ds.setUser("sa");
        h2Ds.setPassword("");
        dataSource = h2Ds;
        createDefaultSchema(h2Ds);
    }

    private static void createDefaultSchema(DataSource ds) {
        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(
                    """
                                CREATE TABLE IF NOT EXISTS registrations (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    reg_key VARCHAR(20) NOT NULL UNIQUE,
                                    disabled BOOLEAN NOT NULL DEFAULT FALSE,
                                    banned BOOLEAN NOT NULL DEFAULT FALSE,
                                    username VARCHAR(40),
                                    email VARCHAR(60),
                                    password VARCHAR(40),
                                    last_used_profile VARCHAR(40)
                                );
                                CREATE TABLE IF NOT EXISTS profiles (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    reg_id INT,
                                    nick VARCHAR(40) NOT NULL UNIQUE,
                                    rating INT NOT NULL DEFAULT 1000,
                                    wins INT NOT NULL DEFAULT 0,
                                    losses INT NOT NULL DEFAULT 0,
                                    invalid INT NOT NULL DEFAULT 0
                                );
                                CREATE TABLE IF NOT EXISTS deleted_profiles (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    reg_id INT,
                                    nick VARCHAR(40),
                                    rating INT,
                                    wins INT,
                                    losses INT,
                                    invalid INT
                                );
                                CREATE TABLE IF NOT EXISTS settings (
                                    property VARCHAR(50) PRIMARY KEY,
                                    value VARCHAR(255) NOT NULL
                                );
                                CREATE TABLE IF NOT EXISTS games (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    player1_name VARCHAR(40),
                                    player1_id INT,
                                    time_create BIGINT,
                                    time_start BIGINT,
                                    time_stop BIGINT,
                                    name VARCHAR(40),
                                    rated BOOLEAN,
                                    speed INT,
                                    size INT,
                                    hills INT,
                                    trees INT,
                                    resources INT,
                                    mapcode INT,
                                    status INT,
                                    winner INT
                                );
                                CREATE TABLE IF NOT EXISTS game_reports (
                                    game_id INT,
                                    tick INT,
                                    team1 INT,
                                    team2 INT,
                                    team3 INT,
                                    team4 INT,
                                    team5 INT,
                                    team6 INT
                                );
                                CREATE TABLE IF NOT EXISTS connections (
                                    game_id INT,
                                    nick1 VARCHAR(40),
                                    nick2 VARCHAR(40),
                                    priority INT
                                );
                                CREATE TABLE IF NOT EXISTS online_profiles (
                                    nick VARCHAR(40) PRIMARY KEY,
                                    game_id INT
                                );
                                CREATE TABLE IF NOT EXISTS messages (
                                    id INT AUTO_INCREMENT PRIMARY KEY,
                                    time TIMESTAMP,
                                    message TEXT
                                );
                                MERGE INTO settings (property, value) KEY(property) VALUES ('max_profiles', '5');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('min_username_length', '2');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('max_username_length', '20');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('min_password_length', '6');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('max_password_length', '20');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('allowed_chars', 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789^|_.,:;?+={}[]()/\\\\&%#!<>*@$');
                                MERGE INTO settings (property, value) KEY(property) VALUES ('revision', '1116');
                            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize default database schema", e);
        }
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

    private static final class DriverManagerDataSource implements DataSource {
        private final String url;
        private final String user;
        private final String password;

        private DriverManagerDataSource(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, user, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(url, username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return DriverManager.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            DriverManager.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) {
            DriverManager.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() {
            return DriverManager.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Cannot unwrap " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
