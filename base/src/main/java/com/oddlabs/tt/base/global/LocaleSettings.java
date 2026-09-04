package com.oddlabs.tt.base.global;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

/**
 * User interface language and locale preferences.
 */
public final class LocaleSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the {@link LocaleSettings} instance registered with the given settings registry.
     *
     * @param settings the settings registry
     * @return the registered locale settings
     */
    public static LocaleSettings from(SettingsRegistry settings) {
        return settings.get(LocaleSettings.class);
    }

    public String language = "default";

    @Override
    public void saveToProperties(Properties props) {
        LocaleSettings defaults = new LocaleSettings();
        SettingsHelper.setProperty(props, "language", language, defaults.language);
    }

    @Override
    public void loadFromProperties(Properties props) {
        language = props.getProperty("language", language);
    }
}
