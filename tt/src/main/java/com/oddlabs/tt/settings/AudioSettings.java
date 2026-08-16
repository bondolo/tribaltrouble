package com.oddlabs.tt.settings;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.settings.SettingsHelper.getBoolean;
import static com.oddlabs.tt.settings.SettingsHelper.getFloat;
import static com.oddlabs.tt.settings.SettingsHelper.setProperty;

/**
 * Audio subsystem configuration, volume levels, and output modes.
 */
public final class AudioSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public boolean play_music = true;
    public boolean play_sfx = true;
    public boolean headphone_mode = false;
    public float music_gain = 0.5f;
    public float sound_gain = 1.0f;
    public boolean warning_no_sound = true;

    @Override
    public void saveToProperties(@NonNull Properties props) {
        AudioSettings defaults = new AudioSettings();
        setProperty(props, "play_music", play_music, defaults.play_music);
        setProperty(props, "play_sfx", play_sfx, defaults.play_sfx);
        setProperty(props, "headphone_mode", headphone_mode, defaults.headphone_mode);
        setProperty(props, "music_gain", music_gain, defaults.music_gain);
        setProperty(props, "sound_gain", sound_gain, defaults.sound_gain);
        setProperty(props, "warning_no_sound", warning_no_sound, defaults.warning_no_sound);
    }

    @Override
    public void loadFromProperties(@NonNull Properties props) {
        play_music = getBoolean(props, "play_music", play_music);
        play_sfx = getBoolean(props, "play_sfx", play_sfx);
        headphone_mode = getBoolean(props, "headphone_mode", headphone_mode);
        music_gain = getFloat(props, "music_gain", music_gain);
        sound_gain = getFloat(props, "sound_gain", sound_gain);
        warning_no_sound = getBoolean(props, "warning_no_sound", warning_no_sound);
    }
}
