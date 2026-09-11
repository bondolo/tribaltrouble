package com.oddlabs.tt.engine.settings;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.base.global.SettingsRegistry;
import com.oddlabs.util.Color;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.getFloat;
import static com.oddlabs.tt.base.global.SettingsHelper.getInt;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * Visual accessibility, color vision deficiency corrections, and player color settings.
 */
public final class AccessibilitySettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Retrieves the {@link AccessibilitySettings} instance registered with the given settings registry.
     *
     * @param settings the settings registry
     * @return the registered accessibility settings
     */
    public static AccessibilitySettings from(SettingsRegistry settings) {
        return settings.get(AccessibilitySettings.class);
    }

    private static final Logger logger = Logger.getLogger(AccessibilitySettings.class.getName());

    public static final Color.Standard[] DEFAULT_PLAYER_COLOURS = {
            new Color.Standard(0xFFFFBF00), /* Orange */
            new Color.Standard(0xFF007FFF), /* Royal Blue */
            new Color.Standard(0xFFFF0040), /* Red */
            new Color.Standard(0xFF00FFBF), /* Teal */
            new Color.Standard(0xFFBF00FF), /* Purple */
            new Color.Standard(0xFFBFFF00) /* Lime */
    };

    public int cvd_mode = 0; // 0=None, 1=Protanopia, 2=Deuteranopia, 3=Tritanopia
    public float cvd_intensity = 1.0f;
    public boolean high_contrast = false;
    public float contrast_intensity = 0.5f;
    public boolean invert_colours = false;
    public float contrast_brightness = 0.0f;
    public float contrast_clarity = 0.0f;
    public boolean team_stencil = false;
    public boolean sound_emojis = true;

    public Color.Standard[] player_colours = Arrays.copyOf(DEFAULT_PLAYER_COLOURS,
            DEFAULT_PLAYER_COLOURS.length);

    public transient Color.Linear[] linear_player_colours = Arrays.stream(player_colours)
            .map(Color.Linear::new)
            .toArray(Color.Linear[]::new);

    /**
     * Sets the player color at the specified index and updates linear player colors.
     *
     * @param index the player index
     * @param colour the standard color
     */
    public void setPlayerColour(int index, Color.Standard colour) {
        player_colours[index] = colour;
        updateLinearColors();
    }

    /**
     * Synchronizes linear player colors from current standard player colors.
     */
    public void updateLinearColors() {
        if (linear_player_colours == null || linear_player_colours.length != player_colours.length) {
            linear_player_colours = new Color.Linear[player_colours.length];
        }
        for (int i = 0; i < player_colours.length; i++) {
            linear_player_colours[i] = new Color.Linear(player_colours[i]);
        }
    }

    @Override
    public void saveToProperties(Properties props) {
        AccessibilitySettings defaults = new AccessibilitySettings();
        setProperty(props, "cvd_mode", cvd_mode, defaults.cvd_mode);
        setProperty(props, "cvd_intensity", cvd_intensity, defaults.cvd_intensity);
        setProperty(props, "high_contrast", high_contrast, defaults.high_contrast);
        setProperty(props, "contrast_intensity", contrast_intensity, defaults.contrast_intensity);
        setProperty(props, "invert_colours", invert_colours, defaults.invert_colours);
        setProperty(props, "contrast_brightness", contrast_brightness, defaults.contrast_brightness);
        setProperty(props, "contrast_clarity", contrast_clarity, defaults.contrast_clarity);
        setProperty(props, "team_stencil", team_stencil, defaults.team_stencil);
        setProperty(props, "sound_emojis", sound_emojis, defaults.sound_emojis);
        setColoursProperty(props, "team_colours", player_colours, defaults.player_colours);
    }

    @Override
    public void loadFromProperties(Properties props) {
        cvd_mode = getInt(props, "cvd_mode", cvd_mode);
        cvd_intensity = getFloat(props, "cvd_intensity", cvd_intensity);
        high_contrast = getBoolean(props, "high_contrast", high_contrast);
        contrast_intensity = getFloat(props, "contrast_intensity", contrast_intensity);
        invert_colours = getBoolean(props, "invert_colours", invert_colours);
        contrast_brightness = getFloat(props, "contrast_brightness", contrast_brightness);
        contrast_clarity = getFloat(props, "contrast_clarity", contrast_clarity);
        team_stencil = getBoolean(props, "team_stencil", team_stencil);
        sound_emojis = getBoolean(props, "sound_emojis", sound_emojis);
        player_colours = getColours(props, "team_colours", player_colours);
        updateLinearColors();
    }

    private static void setColoursProperty(Properties props, String key,
            Color.Standard[] value, Color.Standard[] defaultValue) {
        if (!Arrays.equals(value, defaultValue)) {
            String colors = Arrays.stream(value)
                    .mapToInt(Color.Standard::toInt)
                    .mapToObj(Integer::toHexString)
                    .collect(Collectors.joining(","));
            props.setProperty(key, colors);
        }
    }

    private static Color.Standard[] getColours(Properties props, String key,
            Color.Standard[] defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            String[] hexStrings = value.split(",");
            Color.Standard[] result = new Color.Standard[DEFAULT_PLAYER_COLOURS.length];
            Arrays.setAll(result, i -> {
                if (i < hexStrings.length) {
                    try {
                        int argb = (int) Long.parseLong(hexStrings[i], 16);
                        return new Color.Standard(argb);
                    } catch (NumberFormatException _) {
                        // ignore invalid color constants
                    }
                }
                return new Color.Standard(DEFAULT_PLAYER_COLOURS[i]);
            });
            return result;
        } catch (Exception e) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value
                    + "'. Using default value. Error: " + e);
            return defaultValue;
        }
    }
}
