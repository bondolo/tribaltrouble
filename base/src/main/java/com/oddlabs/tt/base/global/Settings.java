package com.oddlabs.tt.base.global;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.getInt;
import static com.oddlabs.tt.base.global.SettingsHelper.getPath;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * Global game settings coordinator and configuration persistence.
 */
public final class Settings implements Serializable, PropertiesSerializer, SettingsRegistry {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    private final ConcurrentMap<Class<? extends PropertiesSerializer>, PropertiesSerializer> serializers = new ConcurrentHashMap<>();

    private transient final @Nullable Path game_dir;

    public transient Path last_event_log_dir = Path.of("");
    // FIXME: Replace with build version metadata
    public int last_revision = -1;
    public boolean crashed = false;

    private final boolean developer_mode = Boolean.getBoolean("com.oddlabs.tt.developer");
    private boolean has_native_campaign = false;

    public boolean hasNativeCampaign() {
        return developer_mode || has_native_campaign;
    }

    public void setHasNativeCampaign(boolean has_native_campaign) {
        this.has_native_campaign = has_native_campaign;
    }

    public Settings() {
        this(null);
    }

    public Settings(@Nullable Path game_dir) {
        this.game_dir = game_dir;
        initSerializers();
    }

    private final Properties loadedProperties = new Properties();

    private void initSerializers() {
        ServiceLoader<PropertiesSerializer> loader = ServiceLoader.load(PropertiesSerializer.class);
        for (PropertiesSerializer serializer : loader) {
            serializers.put(serializer.getClass(), serializer);
        }
    }

    /**
     * Retrieves a registered {@link PropertiesSerializer} by its class.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertiesSerializer> @NonNull T get(Class<T> type) {
        return (T) serializers.computeIfAbsent(type, t -> {
            try {
                T instance = type.getDeclaredConstructor().newInstance();
                if (!loadedProperties.isEmpty()) {
                    instance.loadFromProperties(loadedProperties);
                }
                return instance;
            } catch (Exception _) {
                throw new NoSuchElementException("No settings component registered for: " + type.getName());
            }
        });
    }

    /**
     * Registers a custom or dynamic {@link PropertiesSerializer}.
     */
    public void registerSerializer(PropertiesSerializer serializer) {
        if (!loadedProperties.isEmpty()) {
            serializer.loadFromProperties(loadedProperties);
        }
        serializers.put(serializer.getClass(), serializer);
    }

    public boolean inDeveloperMode() {
        return developer_mode;
    }

    private boolean playback;

    public void setPlayback(boolean playback) {
        this.playback = playback;
    }

    public boolean isPlayback() {
        return playback;
    }

    public void save() {
        if (playback) {
            return;
        }

        Path settings_file = game_dir != null
                ? game_dir.resolve(AppConfig.SETTINGS_FILE_NAME) : AppConfig.SETTINGS_FILE_NAME;
        try (OutputStream out = Files.newOutputStream(settings_file)) {
            Properties props = new Properties();

            saveToProperties(props);
            props.store(out, Instant.now().toString());
        } catch (IOException e) {
            logger.warning("Failed to write settings to " + settings_file + " exception: " + e);
        }
    }

    public void load() {
        Properties props = new Properties();
        Path settings_file = game_dir != null
                ? game_dir.resolve(AppConfig.SETTINGS_FILE_NAME) : AppConfig.SETTINGS_FILE_NAME;
        if (!Files.exists(settings_file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(settings_file)) {
            props.load(in);
            loadFromProperties(props);
        } catch (IOException _) {
            logger.warning("WARNING: Could not read settings from " + settings_file + ". Using defaults.");
        }
    }

    @Override
    public void saveToProperties(Properties props) {
        props.putAll(loadedProperties);

        Settings defaults = new Settings();

        setProperty(props, "last_event_log_dir", last_event_log_dir, defaults.last_event_log_dir);
        setProperty(props, "last_revision", last_revision, defaults.last_revision);
        setProperty(props, "crashed", crashed, defaults.crashed);
        setProperty(props, "has_native_campaign", has_native_campaign, defaults.has_native_campaign);

        for (PropertiesSerializer serializer : serializers.values()) {
            serializer.saveToProperties(props);
        }
    }

    @Override
    public void loadFromProperties(Properties props) {
        loadedProperties.clear();
        loadedProperties.putAll(props);

        last_event_log_dir = getPath(props, "last_event_log_dir", last_event_log_dir);
        last_revision = getInt(props, "last_revision", last_revision);
        crashed = getBoolean(props, "crashed", crashed);
        has_native_campaign = getBoolean(props, "has_native_campaign", has_native_campaign);

        for (PropertiesSerializer serializer : serializers.values()) {
            serializer.loadFromProperties(props);
        }
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(last_event_log_dir.toString());
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        last_event_log_dir = Path.of((String) in.readObject());
    }
}
