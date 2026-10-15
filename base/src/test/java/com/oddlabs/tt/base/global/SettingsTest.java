package com.oddlabs.tt.base.global;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Settings} persistence, tripwire behavior, and serializer delegation.
 */
final class SettingsTest {

    public static final class TestSerializer implements PropertiesSerializer {
        public int value = 0;
        public boolean flag = false;

        @Override
        public void saveToProperties(Properties props) {
            props.setProperty("test_val", Integer.toString(value));
            props.setProperty("test_flag", Boolean.toString(flag));
        }

        @Override
        public void loadFromProperties(Properties props) {
            value = Integer.parseInt(props.getProperty("test_val", "0"));
            flag = Boolean.parseBoolean(props.getProperty("test_flag", "false"));
        }
    }

    private static final class UninstantiableSerializer implements PropertiesSerializer {
        private UninstantiableSerializer(String requiredArg) {
        }

        @Override
        public void saveToProperties(Properties props) {
        }

        @Override
        public void loadFromProperties(Properties props) {
        }
    }

    @Test
    void testGetRegisteredSettings() {
        Settings settings = new Settings();
        TestSerializer testSerializer = settings.get(TestSerializer.class);
        assertNotNull(testSerializer);
        assertSame(testSerializer, settings.get(TestSerializer.class));
    }

    @Test
    void testGetUnregisteredThrowsNoSuchElementException() {
        Settings settings = new Settings();
        assertThrows(NoSuchElementException.class, () -> settings.get(UninstantiableSerializer.class));
    }

    @Test
    void testRegisterCustomSerializer() {
        Settings settings = new Settings();
        UninstantiableSerializer custom = new UninstantiableSerializer("arg");
        settings.registerSerializer(custom);
        assertSame(custom, settings.get(UninstantiableSerializer.class));
    }

    @Test
    void testLoadPropertiesAppliesToLazyComponents() {
        Settings settings = new Settings();
        Properties props = new Properties();
        props.setProperty("test_val", "42");
        props.setProperty("test_flag", "true");
        settings.loadFromProperties(props);

        TestSerializer testSerializer = settings.get(TestSerializer.class);
        assertEquals(42, testSerializer.value);
        assertTrue(testSerializer.flag);
    }

    @Test
    void testHasNativeCampaignPersistence() {
        Settings settings = new Settings();
        org.junit.jupiter.api.Assertions.assertFalse(settings.hasNativeCampaign());

        settings.setHasNativeCampaign(true);
        org.junit.jupiter.api.Assertions.assertTrue(settings.hasNativeCampaign());

        Properties props = new Properties();
        settings.saveToProperties(props);

        Settings loaded = new Settings();
        loaded.loadFromProperties(props);
        org.junit.jupiter.api.Assertions.assertTrue(loaded.hasNativeCampaign());
    }

    @Test
    void testPurgeDefaultPropertyOverwritesStaleLoadedProperties() {
        Properties initialProps = new Properties();
        initialProps.setProperty("crashed", "true");

        Settings settings = new Settings();
        settings.loadFromProperties(initialProps);
        assertEquals(true, settings.isCrashed());

        // Reset to default value (false)
        settings.setCrashed(false);

        Properties savedProps = new Properties();
        settings.saveToProperties(savedProps);

        // "crashed" should have been removed because it matches the default, rather than preserved from initialProps
        org.junit.jupiter.api.Assertions.assertNull(savedProps.getProperty("crashed"));
    }

    @Test
    void testEncapsulatedFieldsAccessorsAndPersistence() {
        Settings settings = new Settings();
        assertEquals(java.nio.file.Path.of(""), settings.getLastEventLogDir());
        assertEquals(-1, settings.getLastRevision());
        org.junit.jupiter.api.Assertions.assertFalse(settings.isCrashed());

        java.nio.file.Path testPath = java.nio.file.Path.of("/tmp/test_logs");
        settings.setLastEventLogDir(testPath);
        settings.setLastRevision(1234);
        settings.setCrashed(true);

        assertEquals(testPath, settings.getLastEventLogDir());
        assertEquals(1234, settings.getLastRevision());
        assertTrue(settings.isCrashed());

        Properties props = new Properties();
        settings.saveToProperties(props);

        Settings loaded = new Settings();
        loaded.loadFromProperties(props);

        assertEquals(testPath, loaded.getLastEventLogDir());
        assertEquals(1234, loaded.getLastRevision());
        assertTrue(loaded.isCrashed());
    }

    @Test
    void testArmAndDisarmCrashTripwire(@TempDir Path tempDir) throws IOException {
        Settings settings = new Settings(tempDir);
        assertFalse(settings.isCrashed());

        Path testLogDir = Path.of("/tmp/logs");
        boolean wasCrashed = settings.armCrashTripwire(testLogDir);
        assertFalse(wasCrashed);
        assertTrue(settings.isCrashed());
        assertEquals(testLogDir.toAbsolutePath(), settings.getLastEventLogDir());

        Properties diskProps = new Properties();
        try (InputStream in = Files.newInputStream(tempDir.resolve(AppConfig.SETTINGS_FILE_NAME))) {
            diskProps.load(in);
        }
        assertEquals("true", diskProps.getProperty("crashed"));
        assertEquals(testLogDir.toAbsolutePath().toString(), diskProps.getProperty("last_event_log_dir"));

        settings.disarmCrashTripwire();
        assertFalse(settings.isCrashed());

        Properties disarmedProps = new Properties();
        try (InputStream in = Files.newInputStream(tempDir.resolve(AppConfig.SETTINGS_FILE_NAME))) {
            disarmedProps.load(in);
        }
        assertNull(disarmedProps.getProperty("crashed"));
    }

    @Test
    void testArmCrashTripwireWhenPreviouslyCrashed(@TempDir Path tempDir) {
        Settings settings = new Settings(tempDir);
        settings.setCrashed(true);

        Path testLogDir = Path.of("/tmp/logs2");
        boolean wasCrashed = settings.armCrashTripwire(testLogDir);
        assertTrue(wasCrashed);
        assertTrue(settings.isCrashed());
        assertEquals(testLogDir.toAbsolutePath(), settings.getLastEventLogDir());
    }
}
