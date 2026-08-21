package com.oddlabs.tt.input;

import com.oddlabs.tt.base.global.PropertiesSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.getFloat;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * User interface parameters, camera controls, unit behavior preferences, and locale.
 */
public final class ControlSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public boolean invert_camera_pitch = false;
    public boolean aggressive_units = false;
    public float mapmode_delay = 0.5f;
    public float tooltip_delay = 0.5f;
    public float ui_scale = 0.0f; // 0.0 = 100% (Scale 1.0), 1.0 = Max Scale
    public String language = "default";

    @Override
    public void saveToProperties(Properties props) {
        ControlSettings defaults = new ControlSettings();
        setProperty(props, "invert_camera_pitch", invert_camera_pitch, defaults.invert_camera_pitch);
        setProperty(props, "aggressive_units", aggressive_units, defaults.aggressive_units);
        setProperty(props, "mapmode_delay", mapmode_delay, defaults.mapmode_delay);
        setProperty(props, "tooltip_delay", tooltip_delay, defaults.tooltip_delay);
        setProperty(props, "ui_scale", ui_scale, defaults.ui_scale);
        setProperty(props, "language", language, defaults.language);
    }

    @Override
    public void loadFromProperties(Properties props) {
        invert_camera_pitch = getBoolean(props, "invert_camera_pitch", invert_camera_pitch);
        aggressive_units = getBoolean(props, "aggressive_units", aggressive_units);
        mapmode_delay = getFloat(props, "mapmode_delay", mapmode_delay);
        tooltip_delay = getFloat(props, "tooltip_delay", tooltip_delay);
        ui_scale = getFloat(props, "ui_scale", ui_scale);
        language = props.getProperty("language", language);
    }
}
