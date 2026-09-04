package com.oddlabs.tt.engine.settings;

import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsRegistry;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.base.global.SettingsHelper.getInt;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * Graphics detail and visual quality settings.
 */
public final class GraphicsSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public int graphic_detail = AppConfig.DEFAULT_DETAIL_NORMAL;

    public static @NonNull GraphicsSettings from(SettingsRegistry settings) {
        return settings.get(GraphicsSettings.class);
    }

    @Override
    public void saveToProperties(Properties props) {
        GraphicsSettings defaults = new GraphicsSettings();
        setProperty(props, "graphic_detail", graphic_detail, defaults.graphic_detail);
    }

    @Override
    public void loadFromProperties(Properties props) {
        graphic_detail = getInt(props, "graphic_detail", graphic_detail);
    }
}
