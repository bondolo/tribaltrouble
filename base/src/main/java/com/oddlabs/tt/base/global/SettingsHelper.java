package com.oddlabs.tt.base.global;

import org.jspecify.annotations.NonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Utility helper methods for serializing and deserializing typed values in {@link Properties}.
 */
public final class SettingsHelper {
    private static final Logger logger = Logger.getLogger(SettingsHelper.class.getName());

    private SettingsHelper() {
    }

    /**
     * Stores a string property if different from default value.
     */
    public static void setProperty(@NonNull Properties props, @NonNull String key, @NonNull String value,
            @NonNull String defaultValue) {
        if (!value.equals(defaultValue)) {
            props.setProperty(key, value);
        }
    }

    /**
     * Stores an integer property if different from default value.
     */
    public static void setProperty(@NonNull Properties props, @NonNull String key, int value, int defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    /**
     * Stores a float property if different from default value.
     */
    public static void setProperty(@NonNull Properties props, @NonNull String key, float value, float defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    /**
     * Stores a boolean property if different from default value.
     */
    public static void setProperty(@NonNull Properties props, @NonNull String key, boolean value,
            boolean defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    /**
     * Stores a path property if different from default value.
     */
    public static void setProperty(@NonNull Properties props, @NonNull String key, @NonNull Path value,
            @NonNull Path defaultValue) {
        if (!value.equals(defaultValue)) {
            props.setProperty(key, value.toString());
        }
    }

    /**
     * Reads a boolean property with fallback default.
     */
    public static boolean getBoolean(@NonNull Properties props, @NonNull String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Reads an integer property with fallback default.
     */
    public static int getInt(@NonNull Properties props, @NonNull String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }

    /**
     * Reads a float property with fallback default.
     */
    public static float getFloat(@NonNull Properties props, @NonNull String key, float defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException _) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }

    /**
     * Reads a path property with fallback default.
     */
    public static @NonNull Path getPath(@NonNull Properties props, @NonNull String key, @NonNull Path defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException _) {
            logger.warning("Invalid path for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }
}
