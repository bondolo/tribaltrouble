package com.oddlabs.tt.engine.settings;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsRegistry;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

/**
 * Graphics detail and visual quality settings.
 */
public final class GraphicsSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public static @NonNull GraphicsSettings from(SettingsRegistry settings) {
        return settings.get(GraphicsSettings.class);
    }

    @Override
    public void saveToProperties(Properties props) {
    }

    @Override
    public void loadFromProperties(Properties props) {
    }
}
