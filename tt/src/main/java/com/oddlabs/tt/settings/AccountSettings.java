package com.oddlabs.tt.settings;

import com.oddlabs.tt.base.global.PropertiesSerializer;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.settings.SettingsHelper.getBoolean;
import static com.oddlabs.tt.settings.SettingsHelper.setProperty;

/**
 * User account credentials and authentication persistence settings.
 */
public final class AccountSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public @NonNull String username = "";
    public @NonNull String pw_digest = "";
    public boolean remember_login = false;

    @Override
    public void saveToProperties(@NonNull Properties props) {
        AccountSettings defaults = new AccountSettings();
        setProperty(props, "username", username, defaults.username);
        setProperty(props, "pw_digest", pw_digest, defaults.pw_digest);
        setProperty(props, "remember_login", remember_login, defaults.remember_login);
    }

    @Override
    public void loadFromProperties(@NonNull Properties props) {
        username = props.getProperty("username", username);
        pw_digest = props.getProperty("pw_digest", pw_digest);
        remember_login = getBoolean(props, "remember_login", remember_login);
    }
}
