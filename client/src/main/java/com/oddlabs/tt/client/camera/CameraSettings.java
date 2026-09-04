package com.oddlabs.tt.client.camera;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsHelper;
import com.oddlabs.tt.base.global.SettingsRegistry;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

/**
 * Camera inversion and delay parameters.
 */
public final class CameraSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the {@link CameraSettings} instance registered with the given settings registry.
     *
     * @param settings the settings registry
     * @return the registered camera settings
     */
    public static CameraSettings from(SettingsRegistry settings) {
        return settings.get(CameraSettings.class);
    }

    public boolean invert_camera_pitch = false;
    public float mapmode_delay = 0.5f;

    @Override
    public void saveToProperties(Properties props) {
        CameraSettings defaults = new CameraSettings();
        SettingsHelper.setProperty(props, "invert_camera_pitch", invert_camera_pitch, defaults.invert_camera_pitch);
        SettingsHelper.setProperty(props, "mapmode_delay", mapmode_delay, defaults.mapmode_delay);
    }

    @Override
    public void loadFromProperties(Properties props) {
        invert_camera_pitch = SettingsHelper.getBoolean(props, "invert_camera_pitch", invert_camera_pitch);
        mapmode_delay = SettingsHelper.getFloat(props, "mapmode_delay", mapmode_delay);
    }
}
