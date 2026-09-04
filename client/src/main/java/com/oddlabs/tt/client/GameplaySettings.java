package com.oddlabs.tt.client;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsHelper;
import com.oddlabs.tt.base.global.SettingsRegistry;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

/**
 * Player gameplay and unit command behavior preferences.
 */
public final class GameplaySettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the {@link GameplaySettings} instance registered with the given settings registry.
     *
     * @param settings the settings registry
     * @return the registered gameplay settings
     */
    public static GameplaySettings from(SettingsRegistry settings) {
        return settings.get(GameplaySettings.class);
    }

    public boolean aggressive_units = false;

    @Override
    public void saveToProperties(Properties props) {
        GameplaySettings defaults = new GameplaySettings();
        SettingsHelper.setProperty(props, "aggressive_units", aggressive_units, defaults.aggressive_units);
    }

    @Override
    public void loadFromProperties(Properties props) {
        aggressive_units = SettingsHelper.getBoolean(props, "aggressive_units", aggressive_units);
    }
}
