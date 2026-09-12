package com.oddlabs.matchserver;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
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
            = "jdbc:h2:mem:oddlabs;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE";

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
