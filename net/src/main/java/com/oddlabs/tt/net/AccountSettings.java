package com.oddlabs.tt.net;

import com.oddlabs.tt.base.global.PropertiesSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * User account credentials and authentication persistence settings.
 */
public final class AccountSettings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    public String username = "";
    public String pw_digest = "";
    public boolean remember_login = false;

    @Override
    public void saveToProperties(Properties props) {
        AccountSettings defaults = new AccountSettings();
        setProperty(props, "username", username, defaults.username);
        setProperty(props, "pw_digest", pw_digest, defaults.pw_digest);
        setProperty(props, "remember_login", remember_login, defaults.remember_login);
    }

    @Override
    public void loadFromProperties(Properties props) {
        username = props.getProperty("username", username);
        pw_digest = props.getProperty("pw_digest", pw_digest);
        remember_login = getBoolean(props, "remember_login", remember_login);
    }
}
