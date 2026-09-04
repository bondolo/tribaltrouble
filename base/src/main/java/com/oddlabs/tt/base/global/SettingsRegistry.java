package com.oddlabs.tt.base.global;

import java.util.NoSuchElementException;

/**
 * Registry for retrieving modular settings components by type.
 */
public interface SettingsRegistry {
    /**
     * Retrieves a registered {@link PropertiesSerializer} by its class.
     *
     * @param type the settings component class
     * @param <T> the settings component type
     * @return the registered instance
     * @throws NoSuchElementException if no instance is registered for the specified type
     */
    <T extends PropertiesSerializer> T get(Class<T> type) throws NoSuchElementException;
}
