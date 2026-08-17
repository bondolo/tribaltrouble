package com.oddlabs.tt.window;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.getInt;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * Display, resolution, window mode, and presentation settings.
 */
public final class WindowSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public int view_width = -1;
    public int view_height = -1;
    public int view_freq = -1;
    public int view_samples = 4;

    public int new_view_width = -1;
    public int new_view_height = -1;
    public int new_view_freq = -1;
    public int new_view_samples = 4;

    public boolean fullscreen = true;
    public final boolean vsync = true;

    @Override
    public void saveToProperties(@NonNull Properties props) {
        WindowSettings defaults = new WindowSettings();
        setProperty(props, "view_width", view_width, defaults.view_width);
        setProperty(props, "view_height", view_height, defaults.view_height);
        setProperty(props, "view_freq", view_freq, defaults.view_freq);
        setProperty(props, "new_view_width", new_view_width, defaults.new_view_width);
        setProperty(props, "new_view_height", new_view_height, defaults.new_view_height);
        setProperty(props, "new_view_freq", new_view_freq, defaults.new_view_freq);
        setProperty(props, "new_view_samples", new_view_samples, defaults.new_view_samples);
        setProperty(props, "fullscreen", fullscreen, defaults.fullscreen);
        setProperty(props, "view_samples", view_samples, defaults.view_samples);
    }

    @Override
    public void loadFromProperties(@NonNull Properties props) {
        view_width = getInt(props, "view_width", view_width);
        view_height = getInt(props, "view_height", view_height);
        view_freq = getInt(props, "view_freq", view_freq);
        new_view_width = getInt(props, "new_view_width", new_view_width);
        new_view_height = getInt(props, "new_view_height", new_view_height);
        new_view_freq = getInt(props, "new_view_freq", new_view_freq);
        new_view_samples = getInt(props, "new_view_samples", new_view_samples);
        fullscreen = getBoolean(props, "fullscreen", fullscreen);
        view_samples = getInt(props, "view_samples", view_samples);
    }
}
