package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.util.CryptUtils;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for database schema creation, user registration, and query operations.
 */
@NullMarked
final class DBInterfaceTest {

    @BeforeAll
    static void setUpDatabase() {
        DBUtils.initInMemoryDatabase();
    }

    @Test
    void defaultSchemaCreated() throws SQLException {
        try (Connection conn = DBUtils.createDatabaseConnection(); Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM registrations")) {
                assertTrue(rs.next());
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM profiles")) {
                assertTrue(rs.next());
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM settings")) {
                assertTrue(rs.next());
            }
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM messages")) {
                assertTrue(rs.next());
            }
        }
        assertEquals("1116", DBInterface.getSetting("revision"));
        assertEquals("5", DBInterface.getSetting("max_profiles"));
    }

    @Test
    void createUserDirectInsertion() throws SQLException {
        Login login = new Login("alice", "secret123");
        LoginDetails details = new LoginDetails("alice@example.com");

        DBInterface.createUser(login, details, null);

        assertTrue(DBInterface.usernameExists("alice"));
        assertTrue(DBInterface.queryUser("alice", "secret123"));
        assertFalse(DBInterface.queryUser("alice", "wrongpassword"));

        try (Connection conn = DBUtils.createDatabaseConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT reg_key, email, password FROM registrations WHERE username = ?")) {
            stmt.setString(1, "alice");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getString("reg_key"));
                assertEquals("alice@example.com", rs.getString("email"));
                assertEquals(CryptUtils.digest("secret123"), rs.getString("password"));
            }
        }
    }

    @Test
    void duplicateUsernameThrowsException() {
        Login login1 = new Login("bob", "password123");
        LoginDetails details1 = new LoginDetails("bob@example.com");
        DBInterface.createUser(login1, details1, null);

        Login login2 = new Login("bob", "differentpassword");
        LoginDetails details2 = new LoginDetails("bob2@example.com");
        assertThrows(RuntimeException.class, () -> DBInterface.createUser(login2, details2, null));
    }

    @Test
    void usernameExistsCaseInsensitive() {
        Login login = new Login("charlie", "password123");
        LoginDetails details = new LoginDetails("charlie@example.com");
        DBInterface.createUser(login, details, null);

        assertTrue(DBInterface.usernameExists("charlie"));
        assertTrue(DBInterface.usernameExists("CHARLIE"));
        assertTrue(DBInterface.usernameExists("Charlie"));
        assertFalse(DBInterface.usernameExists("nonexistentuser"));
    }

    @Test
    void profileCreationAndLookup() {
        Login login = new Login("david", "password123");
        LoginDetails details = new LoginDetails("david@example.com");
        DBInterface.createUser(login, details, null);

        DBInterface.createProfile("david", "DaveHero");
        assertTrue(DBInterface.nickExists("DaveHero"));
        assertFalse(DBInterface.nickExists("UnknownHero"));

        Profile[] profiles = DBInterface.getProfiles("david", 1116);
        assertEquals(1, profiles.length);
        assertEquals("DaveHero", profiles[0].getNick());

        Profile profile = DBInterface.getProfile("david", "DaveHero", 1116);
        assertNotNull(profile);
        assertEquals("DaveHero", profile.getNick());
        assertEquals(1000, profile.getRating());
    }

    @Test
    void profileLifecycleAndStats() throws SQLException {
        Login login = new Login("eve", "password123");
        LoginDetails details = new LoginDetails("eve@example.com");
        DBInterface.createUser(login, details, null);

        DBInterface.createProfile("eve", "EveChamp");
        DBInterface.setLastUsedProfile("eve", "EveChamp");
        assertEquals("EveChamp", DBInterface.getLastUsedProfile("eve"));

        assertEquals(0, DBInterface.getWins("EveChamp"));
        DBInterface.increaseWins("EveChamp");
        assertEquals(1, DBInterface.getWins("EveChamp"));

        assertEquals(1000, DBInterface.getRating("EveChamp"));
        DBInterface.updateRating("EveChamp", 25);
        assertEquals(1025, DBInterface.getRating("EveChamp"));

        DBInterface.deleteProfile("eve", "EveChamp");
        assertFalse(DBInterface.nickExists("EveChamp"));
        assertNull(DBInterface.getProfile("eve", "EveChamp", 1116));
    }

    @Test
    void onlineProfileTracking() throws SQLException {
        DBInterface.profileOnline("ActivePlayer");
        try (Connection conn = DBUtils.createDatabaseConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT count(*) FROM online_profiles WHERE nick = ?")) {
            stmt.setString(1, "ActivePlayer");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }

        DBInterface.profileOffline("ActivePlayer");
        try (Connection conn = DBUtils.createDatabaseConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT count(*) FROM online_profiles WHERE nick = ?")) {
            stmt.setString(1, "ActivePlayer");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    @Test
    void serverSettingsVerification() {
        assertEquals(2, DBInterface.getSettingsInt("min_username_length"));
        assertEquals(20, DBInterface.getSettingsInt("max_username_length"));
        assertEquals(6, DBInterface.getSettingsInt("min_password_length"));
        assertEquals(20, DBInterface.getSettingsInt("max_password_length"));
        assertEquals(5, DBInterface.getSettingsInt("max_profiles"));
        assertEquals(1116, DBInterface.getSettingsInt("revision"));
        assertNotNull(DBInterface.getSetting("allowed_chars"));
    }

    @Test
    void postHermesMessage() throws SQLException {
        String msg = "System smoke test message";
        DBUtils.postHermesMessage(msg);

        try (Connection conn = DBUtils.createDatabaseConnection(); PreparedStatement stmt = conn.prepareStatement(
                "SELECT message FROM messages WHERE message = ?")) {
            stmt.setString(1, msg);
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(msg, rs.getString("message"));
            }
        }
    }
}
