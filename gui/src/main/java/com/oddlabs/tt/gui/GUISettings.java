package com.oddlabs.tt.gui;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsHelper;
import com.oddlabs.tt.base.global.SettingsRegistry;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

/**
 * User interface scaling and tooltip delay configuration.
 */
public final class GUISettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the {@link GUISettings} instance registered with the given settings registry.
     *
     * @param settings the settings registry
     * @return the registered GUI settings
     */
    public static GUISettings from(SettingsRegistry settings) {
        return settings.get(GUISettings.class);
    }

    public float ui_scale = 0.0f; // 0.0 = 100% (Scale 1.0), 1.0 = Max Scale
    public float tooltip_delay = 0.5f;

    @Override
    public void saveToProperties(Properties props) {
        GUISettings defaults = new GUISettings();
        SettingsHelper.setProperty(props, "ui_scale", ui_scale, defaults.ui_scale);
        SettingsHelper.setProperty(props, "tooltip_delay", tooltip_delay, defaults.tooltip_delay);
    }

    @Override
    public void loadFromProperties(Properties props) {
        ui_scale = SettingsHelper.getFloat(props, "ui_scale", ui_scale);
        tooltip_delay = SettingsHelper.getFloat(props, "tooltip_delay", tooltip_delay);
    }
}
