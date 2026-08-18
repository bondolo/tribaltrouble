package com.oddlabs.tt.settings;

import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.global.AppConfig;
import com.oddlabs.tt.base.global.PropertiesSerializer;
import com.oddlabs.tt.input.ControlSettings;
import com.oddlabs.tt.net.AccountSettings;
import com.oddlabs.tt.window.WindowSettings;
import com.oddlabs.util.Color;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.oddlabs.tt.base.global.SettingsHelper.getBoolean;
import static com.oddlabs.tt.base.global.SettingsHelper.getInt;
import static com.oddlabs.tt.base.global.SettingsHelper.getPath;
import static com.oddlabs.tt.base.global.SettingsHelper.setProperty;

/**
 * Global game settings coordinator and configuration persistence.
 */
public final class Settings implements Serializable, PropertiesSerializer {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    public static final Color.Standard[] DEFAULT_TEAM_COLOURS = AccessibilitySettings.DEFAULT_TEAM_COLOURS;

    private final Map<Class<? extends PropertiesSerializer>, PropertiesSerializer> serializers = new LinkedHashMap<>();

    private transient final @Nullable Path game_dir;

    public final @NonNull WindowSettings window;
    public final @NonNull AudioSettings audio;
    public final @NonNull AccountSettings account;
    public final @NonNull AccessibilitySettings accessibility;
    public final @NonNull ControlSettings control;

    public transient @NonNull Path last_event_log_dir = Path.of("");
    public int last_revision = -1;
    public boolean crashed = false;

    public int graphic_detail = AppConfig.DEFAULT_DETAIL_NORMAL;

    private final boolean developer_mode = Boolean.getBoolean("com.oddlabs.tt.developer");
    public boolean has_native_campaign = developer_mode;

    public final boolean save_event_log = true;
    public boolean generate_dummy_worlds = false;
    public boolean first_run = true;

    public final boolean hide_multiplayer = true;
    public final int frame_grab_milliseconds_per_frame = 40;

    public Settings() {
        this(null);
    }

    public Settings(@Nullable Path game_dir) {
        this.game_dir = game_dir;
        initSerializers();
        this.window = getOrCreate(WindowSettings.class, WindowSettings::new);
        this.audio = getOrCreate(AudioSettings.class, AudioSettings::new);
        this.account = getOrCreate(AccountSettings.class, AccountSettings::new);
        this.accessibility = getOrCreate(AccessibilitySettings.class, AccessibilitySettings::new);
        this.control = getOrCreate(ControlSettings.class, ControlSettings::new);
    }

    private void initSerializers() {
        ServiceLoader<PropertiesSerializer> loader = ServiceLoader.load(PropertiesSerializer.class);
        for (PropertiesSerializer serializer : loader) {
            serializers.put(serializer.getClass(), serializer);
        }
    }

    /**
     * Retrieves a registered {@link PropertiesSerializer} by its class.
     */
    @SuppressWarnings("unchecked")
    public <T extends PropertiesSerializer> @NonNull T get(@NonNull Class<T> type) {
        T serializer = (T) serializers.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("No PropertiesSerializer registered for: " + type.getName());
        }
        return serializer;
    }

    /**
     * Retrieves a registered {@link PropertiesSerializer} by its class, or creates and registers a new instance.
     */
    @SuppressWarnings("unchecked")
    public <T extends PropertiesSerializer> @NonNull T getOrCreate(@NonNull Class<T> type,
            @NonNull Supplier<T> fallback) {
        T serializer = (T) serializers.get(type);
        if (serializer == null) {
            serializer = fallback.get();
            serializers.put(type, serializer);
        }
        return serializer;
    }

    /**
     * Registers a custom or dynamic {@link PropertiesSerializer}.
     */
    public void registerSerializer(@NonNull PropertiesSerializer serializer) {
        serializers.put(serializer.getClass(), serializer);
    }

    public boolean inDeveloperMode() {
        return developer_mode;
    }

    public int getTexelsPerGridUnit() {
        return AppConfig.DEFAULT_TEXELS_PER_GRID_UNIT / (int) Math.pow(2,
                AppConfig.DEFAULT_TEXTURE_MIP_SHIFT[graphic_detail]);
    }

    public void save() {
        if (LocalEventQueue.getQueue().getDeterministic().isPlayback())
            return;

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
    public void saveToProperties(@NonNull Properties props) {
        Settings defaults = new Settings();

        setProperty(props, "last_event_log_dir", last_event_log_dir, defaults.last_event_log_dir);
        setProperty(props, "last_revision", last_revision, defaults.last_revision);
        setProperty(props, "crashed", crashed, defaults.crashed);
        setProperty(props, "graphic_detail", graphic_detail, defaults.graphic_detail);
        setProperty(props, "first_run", first_run, defaults.first_run);

        for (PropertiesSerializer serializer : serializers.values()) {
            serializer.saveToProperties(props);
        }
    }

    @Override
    public void loadFromProperties(@NonNull Properties props) {
        last_event_log_dir = getPath(props, "last_event_log_dir", last_event_log_dir);
        last_revision = getInt(props, "last_revision", last_revision);
        crashed = getBoolean(props, "crashed", crashed);
        graphic_detail = getInt(props, "graphic_detail", graphic_detail);
        first_run = getBoolean(props, "first_run", first_run);

        for (PropertiesSerializer serializer : serializers.values()) {
            serializer.loadFromProperties(props);
        }
    }

    @Serial
    private void writeObject(@NonNull ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(last_event_log_dir.toString());
    }

    @Serial
    private void readObject(@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        last_event_log_dir = Path.of((String) in.readObject());
    }
}
