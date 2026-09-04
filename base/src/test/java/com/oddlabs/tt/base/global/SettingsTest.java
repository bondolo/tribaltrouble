package com.oddlabs.tt.base.global;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
