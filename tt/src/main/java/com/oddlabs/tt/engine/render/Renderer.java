package com.oddlabs.tt.engine.render;

import com.oddlabs.event.Deterministic;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.animation.TimerAnimation;
import com.oddlabs.tt.base.animation.Updatable;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.Globals;
import com.oddlabs.tt.settings.Settings;
import com.oddlabs.tt.base.util.StatCounter;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.engine.render.state.GLRenderContext;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.resource.NativeResource;
import com.oddlabs.tt.engine.resource.Resources;
import com.oddlabs.tt.engine.util.GLUtils;
import com.oddlabs.tt.engine.vbo.VBO;
import com.oddlabs.tt.window.LWJGL3Window;
import com.oddlabs.tt.window.Window;
import com.oddlabs.tt.net.Network;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * The main rendering engine and application controller.
 * Manages the game loop, window lifecycle, input handling, and audio coordination.
 */
public final class Renderer implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(Renderer.class.getSimpleName());
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final boolean PROFILE = Boolean.getBoolean("com.oddlabs.tt.profile");

    /** This is expected to be initialized before Locale.setDefault is ever called */
    private static final Locale default_locale = Locale.of(Locale.getDefault().getLanguage(), Locale.getDefault()
            .getCountry(), "default");

    private static final ResourceBundle bundle = ResourceBundle.getBundle(Renderer.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final int INSTRUMENTATION_FRAME_COUNT = Integer.MAX_VALUE;

    private static final Renderer renderer_instance = new Renderer();

    private static final StatCounter fps = new StatCounter(10);
    private static int num_triangles_rendered;
    private static boolean grab_frames = false;

    private static volatile boolean finished = false;

    private final GamePaths gamePaths;
    private final @NonNull Settings settings;
    private final Locale locale = default_locale;

    private final GLRenderContext renderContext = new GLRenderContext();
    private final Window window = new LWJGL3Window();
    private final Network network = new Network();

    private int lastDisplayW = -1;
    private int lastDisplayH = -1;

    // Currently only OpenAL is supported
    private @Nullable OpenALManager audioManager;

    private AudioPlayer music;
    private @Nullable AudioParameters music_audio;
    private @Nullable TimerAnimation music_timer;

    private boolean movie_recording_started = false;

    private int instrumentationFrameCounter;
    private long totalPollEventsTime;
    private long totalRunGameLoopTime;
    private long totalAudioUpdateTime;
    private long totalWindowUpdateTime;
    private long totalDisplayTime;
    private long totalGLFinishTime;
    private long totalLoopTime;

    private Renderer() {
        logger.info("CWD: " + System.getProperty("user.dir"));
        // This will be configured by setupLogging, but we need to log before that.
        try {
            gamePaths = setupPaths();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        settings = new Settings(gamePaths.dataDir());
    }

    public static float getFPS() {
        return fps.getAveragePerUpdate();
    }

    public static boolean isRegistered() {
        return true;
    }

    public static void makeCurrent() {
        try {
            getRenderer().getWindow().makeCurrent();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to make OpenGL context current", e);
        }
    }

    @Override
    public void close() {
        logger.info("Closing AudioManager...");
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }
        logger.info("Closing Window...");
        window.close();
    }

    public @NonNull Window getWindow() {
        return window;
    }

    public static @NonNull Renderer getRenderer() {
        return renderer_instance;
    }

    public @NonNull Settings getSettings() {
        return settings;
    }

    public @NonNull AudioManager getAudioManager() {
        AudioManager manager = audioManager;
        if (null == manager) {
            throw new IllegalStateException("AudioManager not initialized");
        }
        return manager;
    }

    /** global sound enable */
    public void startSound() {
        Objects.requireNonNull(audioManager).startSources();
    }

    /** global sound disable */
    public void stopSound() {
        Objects.requireNonNull(audioManager).stopSources();
    }

    public @NonNull LocalEventQueue getEventQueue() {
        return LocalEventQueue.getQueue();
    }

    public @NonNull Network getNetwork() {
        return network;
    }

    public @NonNull RenderContext getRenderContext() {
        return renderContext;
    }

    private void runGameLoop(@NonNull NetworkSelector network, @NonNull FrameDriver driver) {
        if (AnimationManager.isTimeFrozen() && !AnimationManager.isTimeStopped())
            AnimationManager.unfreezeTime();
        long current_time;
        if (grab_frames) {
            AnimationManager.warpTime(settings.frame_grab_milliseconds_per_frame);
            current_time = AnimationManager.getSystemTime();
        } else {
            current_time = AnimationManager.getSystemTime();
        }
        long last_frame_time = AnimationManager.getLastFrameTime();
        long time_diff = current_time - last_frame_time;
        AnimationManager.setLastFrameTime(current_time);
        Deterministic deterministic = getEventQueue().getDeterministic();
        if (time_diff > AnimationManager.MAX_STEP_MILLIS && !java.util.Objects.requireNonNull(deterministic)
                .isPlayback()) {
            java.util.logging.Logger.getLogger(Renderer.class.getName()).warning("Skipping large time diff: "
                    + time_diff + " ms.");
            time_diff = 0;
        }

        AnimationManager.getFrameTimeCounter().updateAbsolute(time_diff);
        AnimationManager.addExecutionTimePrecision(AnimationManager.getFrameTimeCounter().getAveragePerUpdate());
        java.util.Objects.requireNonNull(deterministic).setEnabled(true);
        while (AnimationManager.getExecutionTimePrecision()
                >= AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK && !isFinished()) {
            AnimationManager.addExecutionTimePrecision(
                    (float) -AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK);
            AnimationManager.addExecutionTime(AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK);
            getEventQueue().tickHighPrecision(AnimationManager.ANIMATION_SECONDS_PER_PRECISION_TICK);
            while (AnimationManager.getExecutionTime() >= AnimationManager.ANIMATION_MILLISECONDS_PER_TICK
                    && !isFinished()) {
                network.tick();

                driver.tick(network);
                if (deterministic.log(getWindow().isOpen() && getWindow().isCloseRequested())) {
                    getWindow().setCloseRequested(false);
                    driver.onCloseRequested();
                }
                AnimationManager.pathfindsPerTick.updateAbsolute(
                        com.oddlabs.tt.simulation.pathfinder.PathFinder.stat_pathfinder_per_frame);
                com.oddlabs.tt.simulation.pathfinder.PathFinder.stat_pathfinder_per_frame = 0;
                getEventQueue().tickLowPrecision(AnimationManager.ANIMATION_SECONDS_PER_TICK);
                AnimationManager.addExecutionTime(-AnimationManager.ANIMATION_MILLISECONDS_PER_TICK);
                AnimationManager.addChecksumMillisecondCounter(AnimationManager.ANIMATION_MILLISECONDS_PER_TICK);
                if (AnimationManager.getChecksumMillisecondCounter()
                        >= AnimationManager.ANIMATION_MILLISECONDS_PER_CHECKSUM) {
                    AnimationManager.addChecksumMillisecondCounter(
                            -AnimationManager.ANIMATION_MILLISECONDS_PER_CHECKSUM);
                    int checksum = getEventQueue().computeChecksum();
                    int logged_checksum = deterministic.log(checksum);
                    if (checksum != logged_checksum && AnimationManager.shouldComplainChecksum()) {
                        java.util.logging.Logger.getLogger(Renderer.class.getName()).severe(
                                "********** ERROR: Checksum mismatch at tick " + getEventQueue()
                                        .getHighPrecisionManager().getTick() + " | checksum = " + checksum
                                        + " | logged_checksum = " + logged_checksum + " **********");
                        AnimationManager.setChecksumComplain(false);
                    }
                }
                if (!Globals.frustum_freeze) {
                    driver.pickHover();
                }
            }
        }
        deterministic.setEnabled(false);
    }

    public static void registerTrianglesRendered(int count) {
        num_triangles_rendered += count;
    }

    public static int getTrianglesRendered() {
        return num_triangles_rendered;
    }

    private void display(@NonNull FrameDriver driver) {
        num_triangles_rendered = 0;
        fps.updateDelta(System.currentTimeMillis());
        NativeResource.processGLCleanupTasks();
        GLUtils.checkGLError("After Cleanup");

        int w = window.getWidth();
        int h = window.getHeight();
        if (w != lastDisplayW || h != lastDisplayH) {
            logger.info("[Renderer] display() viewport changed: " + lastDisplayW + "x" + lastDisplayH + " -> " + w + "x"
                    + h);
            lastDisplayW = w;
            lastDisplayH = h;
        }
        // Ensure viewport is correct for the main pass
        renderContext.setViewport(0, 0, w, h);

        driver.render();

        if (Globals.debugRenderingEnabled()) {
            renderContext.validate();
        }
    }

    public void updateProgress(@NonNull FrameDriver driver) {
        renderContext.reset(); // Fix texture bleeding
        display(driver);
        window.update();
        window.pollEvents();
    }

    public static void shutdown() {
        finished = true;
    }

    public static boolean isFinished() {
        return finished;
    }

    private static void deleteLog(@NonNull Path log) throws IOException {
        for (Path LOG_FILES : com.oddlabs.util.Utils.LOG_FILES) {
            Path log_file = log.resolve(LOG_FILES);
            Files.deleteIfExists(log_file);
        }
        Files.deleteIfExists(log);
    }

    private static void deleteOldLogs(File last_log_dir, File new_log_dir, @NonNull File logs_dir) {
        File[] logs = logs_dir.listFiles();
        if (logs == null)
            return;
        for (File log : logs)
            try {
                if (!log.isDirectory() || log.equals(last_log_dir) || log.equals(new_log_dir))
                    continue;
                deleteLog(log.toPath());
            } catch (IOException _) {
                /* ignore */
            }
    }

    /**
     * Returns a directory path for the specified system property value or, if
     * the property or path is unavailable, the path of a temp directory.
     *
     * @param property name of the system property
     * @return Path to a directory or null if filesystem operations don't
     *         generally seem to work.
     */
    public static @Nullable Path getPropertyPath(@NonNull String property) {
        String propertyValue;
        try {
            propertyValue = System.getProperty(property);
        } catch (SecurityException disallowed) {
            // We are probably sandboxed.
            propertyValue = null;
        }
        Path result = null;
        if (null != propertyValue) {
            try {
                result = Path.of(propertyValue);
                if (Files.notExists(result))
                    result = Files.createDirectories(result);
                if (!Files.isDirectory(result) || !Files.isReadable(result)) {
                    // Path is not something we can use, fall back to temp
                    result = null;
                }
            } catch (IOException badnews) {
                result = null;
            }
        }

        if (null == result) {
            // whelp, let's use a temp directory if we can.
            try {
                result = Files.createTempDirectory(property);
            } catch (IOException | SecurityException totalFailure) {
                logger.log(Level.WARNING, "Failed to create temp directory for " + property, totalFailure);
            }
        }

        return result;
    }

    /**
     * Returns the user's home directory or if that is unavailable a temp
     * directory.
     *
     * @return Path to a directory or null if filesystem operations don't
     *         generally seem to work.
     */
    public static @Nullable Path getUserHomePath() {
        return getPropertyPath("user.home");
    }

    private static boolean isUsable(@Nullable Path path) {
        if (path == null) return false;
        try {
            if (!Files.exists(path) || !Files.isDirectory(path) || !Files.isWritable(path)) {
                return false;
            }
            // Try creating a temporary file to be absolutely sure we can actually write.
            // Some sandboxes (like macOS Seatbelt) might allow isWritable to return true
            // but block the actual creation of new files.
            try {
                Path testFile = Files.createTempFile(path, ".tt_write_test", null);
                Files.delete(testFile);
                return true;
            } catch (IOException | SecurityException e) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public record GamePaths(Path dataDir, Path logDir) {
    }

    private static @NonNull GamePaths setupPaths() throws IOException {
        Path dataDir = null;
        Path logDir = null;
        boolean portable = false;

        // 1. Check for Portable Mode (CWD)
        // If a game directory exists in the current working directory, use it.
        Path localDir = Path.of(Globals.GAME_NAME);
        if (isUsable(localDir)) {
            dataDir = localDir;
            portable = true;
        }

        // 2. Check for Portable Mode (App/JAR Directory)
        // If the JAR is launched from a different CWD, check next to the JAR.
        if (!portable) {
            try {
                java.security.CodeSource codeSource = Renderer.class.getProtectionDomain().getCodeSource();
                if (codeSource != null) {
                    Path jarPath = Path.of(codeSource.getLocation().toURI());
                    Path appDir = jarPath.getParent();
                    if (appDir != null) {
                        Path appGameDir = appDir.resolve(Globals.GAME_NAME);
                        // Avoid checking the same path twice if CWD == AppDir
                        if (!appGameDir.equals(localDir.toAbsolutePath()) && isUsable(appGameDir)) {
                            dataDir = appGameDir;
                            portable = true;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors determining app directory
            }
        }

        String os_name;
        try {
            os_name = System.getProperty("os.name").toLowerCase();
        } catch (SecurityException e) {
            os_name = "unknown";
        }

        Path userHome = getUserHomePath();

        // 3. Resolve Data Directory (if not portable)
        if (!portable) {
            String xdgConfigHome = null;
            String appData = null;
            try {
                xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
                appData = System.getenv("APPDATA");
            } catch (SecurityException _) {
                /* ignore */
            }

            Path preferred = null;
            Path fallback = null;
            Path existing = null;

            if (os_name.contains("mac")) {
                if (userHome != null) {
                    Path appSupport = userHome.resolve("Library/Application Support/" + Globals.GAME_NAME);
                    Path config = userHome.resolve(".config/tribaltrouble");

                    if (isUsable(appSupport)) existing = appSupport;
                    else if (isUsable(config)) existing = config;

                    preferred = appSupport;
                    fallback = config;
                }
            } else if (os_name.contains("linux") || os_name.contains("unix")) {
                Path legacyDot = userHome != null ? userHome.resolve(".tribaltrouble") : null;
                Path currentDot = Path.of(".tribaltrouble");

                Path xdg;
                if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
                    xdg = Path.of(xdgConfigHome).resolve("tribaltrouble");
                } else if (userHome != null) {
                    xdg = userHome.resolve(".config/tribaltrouble");
                } else {
                    xdg = null;
                }

                if (isUsable(legacyDot)) existing = legacyDot;
                else if (isUsable(currentDot)) existing = currentDot;
                else if (isUsable(xdg)) existing = xdg;

                preferred = xdg;
                fallback = legacyDot;
            } else {
                Path roaming = appData != null ? Path.of(appData).resolve(Globals.GAME_NAME) : null;
                Path homeGame = userHome != null ? userHome.resolve(Globals.GAME_NAME) : null;

                if (isUsable(roaming)) existing = roaming;
                else if (isUsable(homeGame)) existing = homeGame;

                preferred = roaming;
                fallback = homeGame;
            }

            if (existing != null) {
                dataDir = existing;
            } else if (preferred != null) {
                try {
                    Files.createDirectories(preferred);
                    if (isUsable(preferred)) dataDir = preferred;
                } catch (IOException | SecurityException e) {
                    // Ignore
                }
            }

            if (dataDir == null && fallback != null) {
                try {
                    Files.createDirectories(fallback);
                    if (isUsable(fallback)) dataDir = fallback;
                } catch (IOException | SecurityException e) {
                    // Ignore
                }
            }

            if (dataDir == null) {
                dataDir = Files.createTempDirectory(Globals.GAME_NAME);
            }
        }

        // 4. Resolve Log Directory
        if (portable) {
            logDir = dataDir.resolve("logs");
        } else {
            // Try standard OS log locations first
            Path preferredLog = null;
            if (os_name.contains("mac")) {
                if (userHome != null) {
                    preferredLog = userHome.resolve("Library/Logs/TribalTrouble");
                }
            } else if (os_name.contains("linux") || os_name.contains("unix")) {
                String xdgStateHome = null;
                try {
                    xdgStateHome = System.getenv("XDG_STATE_HOME");
                } catch (SecurityException _) {
                    /* ignore */
                }

                if (xdgStateHome != null && !xdgStateHome.isEmpty()) {
                    preferredLog = Path.of(xdgStateHome).resolve("tribaltrouble/logs");
                } else if (userHome != null) {
                    preferredLog = userHome.resolve(".local/state/tribaltrouble/logs");
                }
            } else {
                // Windows
                String localAppData = null;
                try {
                    localAppData = System.getenv("LOCALAPPDATA");
                } catch (SecurityException _) {
                    /* ignore */
                }

                if (localAppData != null) {
                    preferredLog = Path.of(localAppData).resolve("TribalTrouble\\logs");
                }
            }

            if (preferredLog != null) {
                try {
                    Files.createDirectories(preferredLog);
                    if (isUsable(preferredLog)) {
                        logDir = preferredLog;
                    }
                } catch (IOException | SecurityException e) {
                    // Ignore
                }
            }

            // Fallback to dataDir/logs if standard location fails
            if (logDir == null) {
                logDir = dataDir.resolve("logs");
            }
        }

        return new GamePaths(dataDir, logDir);
    }

    public @NonNull GamePaths getGamePaths() {
        return gamePaths;
    }

    public void run(@NonNull ClientStartup startup, @NonNull String @NonNull... args)
            throws IOException {
        Instant start_time = Instant.now();
        boolean first_frame = true;
        logger.info("********** Running tt **********");
        logger.info("game dir: " + gamePaths.dataDir());
        logger.info("logs dir: " + gamePaths.logDir());
        boolean eventload = false;
        boolean zipped = false;
        boolean silent = false;
        for (int i = 0; i < args.length; i++)
            switch (args[i]) {
            case "--grabframes" -> grab_frames = true;
            case "--eventload" -> {
                eventload = true;
                i++;
                switch (args[i]) {
                    case "zipped":
                        zipped = true;
                        break;
                    case "normal":
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown event load mode: " + args[i]);
                }
            }
            case "--silent" -> silent = true;
            default -> throw new IllegalArgumentException("Unknown command line flag: " + args[i]);
            }

        Settings settings = getSettings();
        settings.load();

        if (eventload || grab_frames) {
            Path last_event_log_path = settings.last_event_log_dir.resolve(zipped ? "event.log.gz" : "event.log");
            logger.info("last_event_log_path = " + last_event_log_path);
            // Only use when anal debugging
//			ChecksumLogger.initLogging();
            getEventQueue().loadEvents(last_event_log_path, zipped);
        }

        Path event_logs_dir = gamePaths.logDir();
        Path event_log_dir = event_logs_dir.resolve(Long.toString(System.currentTimeMillis()));
        if (settings.save_event_log) {
            setupLogging(event_log_dir, silent);
            getEventQueue().setEventsLogged(event_log_dir.resolve(com.oddlabs.util.Utils.EVENT_LOG));
        }
        Deterministic deterministic = getEventQueue().getDeterministic();
        var game_dir = deterministic.log(gamePaths.dataDir);
        event_log_dir = deterministic.log(event_log_dir);
        deterministic.log(settings);
        Locale language = "default".equals(settings.control.language)
                ? deterministic.log(Renderer.default_locale) : Locale.forLanguageTag(settings.control.language);
        if (language == null) {
            language = Locale.of("en");
        }
        IO.println("Using language " + language);
        Locale.setDefault(language);

        Path last_event_log_dir = settings.last_event_log_dir;
        boolean crashed = settings.crashed;
        NetworkSelector network = new NetworkSelector(getEventQueue().getDeterministic(), getEventQueue()::getMillis);
        try {
            initNative(crashed);
        } catch (Exception e) {
            // Let it propagate
            throw new IllegalStateException("Failed initializing natives", e);
        }

        if (!settings.inDeveloperMode() && !deterministic.isPlayback())
            deleteOldLogs(last_event_log_dir.toFile(), event_log_dir.toFile(), event_logs_dir.toFile());

        Duration startup_time_init = Duration.between(start_time, Instant.now());
        logger.info("Init done after " + startup_time_init + "ms");

        ClientStartup.Session session = startup.init(network, true);
        FrameDriver driver = session.driver();
        Runnable load_task = session.loadTask();

        boolean wasActive = window.isActive();
        try {
            while (!finished) {
                long frameStart = System.nanoTime();

                // Use a small timeout when not the active window to save CPU
                boolean isActive = window.isActive();
                long t0 = System.nanoTime();
                if (isActive) {
                    window.pollEvents();
                } else {
                    ((LWJGL3Window) window).pollEvents(100);
                }
                long t1 = System.nanoTime();
                totalPollEventsTime += (t1 - t0);

                if (isActive && !wasActive) {
                    logger.info("[Renderer] Focus Gained (isActive=true, wasActive=" + wasActive + ")");
                    if (window.isIconified()) {
                        window.restore();
                    }
                    if (!window.isVisible()) {
                        window.show();
                    }
                    window.focus();
                } else if (!isActive && wasActive) {
                    logger.info("[Renderer] Focus Lost (isActive=false, wasActive=" + wasActive + ")");
                    if (getSettings().window.fullscreen) {
                        window.minimize();
                    }
                }
                wasActive = isActive;

                long t2 = System.nanoTime();
                runGameLoop(network, driver);
                long t3 = System.nanoTime();
                totalRunGameLoopTime += (t3 - t2);

                long t4 = System.nanoTime();
                if (audioManager != null) {
                    audioManager.update(AnimationManager.ANIMATION_SECONDS_PER_TICK);
                }
                long t5 = System.nanoTime();
                totalAudioUpdateTime += (t5 - t4);

                getAudioManager().setMasterGain(isActive ? 1f : 0f);
                long t6 = System.nanoTime();
                if (!first_frame && window.isVisible()) {
                    window.update();
                }
                long t7 = System.nanoTime();
                totalWindowUpdateTime += (t7 - t6);

                long t8 = System.nanoTime();
                display(driver);
                long t9 = System.nanoTime();
                totalDisplayTime += (t9 - t8);

                if (PROFILE) {
                    long tf0 = System.nanoTime();
                    GL11.glFinish();
                    long tf1 = System.nanoTime();
                    totalGLFinishTime += (tf1 - tf0);
                }

                if (first_frame) {
                    Duration startup_time = Duration.between(start_time, Instant.now());
                    logger.info("First frame rendered after " + startup_time);
                    first_frame = false;
                    if (load_task != null) {
                        window.update();
                        getEventQueue().getDeterministic().setEnabled(true);
                        try {
                            load_task.run();
                        } finally {
                            getEventQueue().getDeterministic().setEnabled(false);
                            renderContext.reset(); // Fix texture bleeding after loading
                        }
                        load_task = null;
                    }
                }
                if (grab_frames && movie_recording_started)
                    GLUtils.takeScreenshot("");

                long frameEnd = System.nanoTime();
                totalLoopTime += (frameEnd - frameStart);

                instrumentationFrameCounter++;
                if (DEBUG && (finished || instrumentationFrameCounter >= INSTRUMENTATION_FRAME_COUNT)) {
                    logger.info(String.format(Locale.ROOT,
                            "[Instrumentation] Averages over %d frames: "
                                    + "Total frame: %.2f ms | "
                                    + "pollEvents: %.2f ms | "
                                    + "runGameLoop: %.2f ms | "
                                    + "audioUpdate: %.2f ms | "
                                    + "windowUpdate: %.2f ms | "
                                    + "display: %.2f ms | "
                                    + "glFinish: %.2f ms",
                            instrumentationFrameCounter,
                            (totalLoopTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalPollEventsTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalRunGameLoopTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalAudioUpdateTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalWindowUpdateTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalDisplayTime / (float) instrumentationFrameCounter) / 1_000_000f,
                            (totalGLFinishTime / (float) instrumentationFrameCounter) / 1_000_000f));

                    instrumentationFrameCounter = 0;
                    totalPollEventsTime = 0;
                    totalRunGameLoopTime = 0;
                    totalAudioUpdateTime = 0;
                    totalWindowUpdateTime = 0;
                    totalDisplayTime = 0;
                    totalGLFinishTime = 0;
                    totalLoopTime = 0;
                }
            }

            getEventQueue().getDeterministic().setEnabled(true);
            getSettings().save();
        } finally {
            cleanup();
        }
    }

    public @NonNull Locale getDefaultLocale() {
        return default_locale;
    }

    private void setupLogging(@NonNull Path event_log_dir, boolean silent) throws IOException {
        try {
            Files.createDirectories(event_log_dir);
            logger.info("Writing log files in " + event_log_dir);

            // Get the root logger and remove default handlers to prevent duplicate console output
            Logger rootLogger = Logger.getLogger("");
            for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Add a file handler
            FileHandler fileHandler = new FileHandler(event_log_dir.resolve("output.log").toString());
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);

            // Add a console handler unless in silent mode
            if (!silent) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new SimpleFormatter());
                rootLogger.addHandler(consoleHandler);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to setup file logging", e);
            throw e;
        }
    }

    private static void failedOpenGL(@NonNull Exception e) {
        try {
            logger.log(Level.SEVERE, "OpenGL Failure", e);
            Main.fail(e);
        } finally {
            Main.shutdown(1);
        }
    }

    public void startMovieRecording() {
        logger.info("ACTION! Movie recording started.");
        movie_recording_started = true;
    }

    public void cleanup() {
        logger.info("Cleaning up...");
        logger.info("Disposing LocalEventQueue...");
        getEventQueue().close();
        destroyNative();
        logger.fine("Native resources still registered: " + NativeResource.getCount());
        logger.info("Cleanup complete. Exiting");
    }

    public @NonNull SerializableDisplayMode getCurrentDisplayMode() {
        return getEventQueue().getDeterministic()
                .log(window.getDisplayMode());
    }

    public void toggleFullscreen() {
        try {
            boolean fs = !window.isFullscreen() && !getEventQueue().getDeterministic().isPlayback();
            logger.info("Toggling fullscreen to: " + fs + ". Current mode: " + window.getDisplayMode());
            window.setFullscreen(fs);
            getSettings().window.fullscreen = fs;
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Mode switching failed with exception", e);
            throw new IllegalStateException("Mode switching failed", e);
        }
    }

    public void switchMode(@NonNull SerializableDisplayMode mode, boolean switch_now) {
        if (switch_now) {
            try {
                window.setDisplayMode(mode);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            modeSwitchedNow(mode);
        } else
            modeSwitchedLater(mode);
    }

    public void setModeToNearest(@NonNull SerializableDisplayMode mode) {
        // Use window create to ensure window is created/resized
        boolean fs = getSettings().window.fullscreen;
        window.create(mode, fs);
        modeSwitchedNow(mode);
    }

    private void modeSwitchedLater(@NonNull SerializableDisplayMode new_mode) {
        settings.window.fullscreen = window.isFullscreen();
        settings.window.new_view_width = new_mode.getWidth();
        settings.window.new_view_height = new_mode.getHeight();
        settings.window.new_view_freq = new_mode.getFrequency();
    }

    private void modeSwitchedNow(@NonNull SerializableDisplayMode new_mode) {
        modeSwitchedLater(new_mode);
        modeSwitched();
    }

    private void modeSwitched() {
        SerializableDisplayMode new_mode = getCurrentDisplayMode();
        logger.info("Mode switch detected. New mode: " + new_mode);
        settings.window.view_width = new_mode.getWidth();
        settings.window.view_height = new_mode.getHeight();
        settings.window.view_freq = new_mode.getFrequency();
    }

    private static void destroyNative() {
        logger.info("Clearing Resources...");
        Resources.clearResources();

        logger.info("Closing AudioManager...");
        var audioManager = getRenderer().audioManager;
        if (null != audioManager) {
            audioManager.close();
        }

        logger.info("Renderer Closed.");
    }

    public static void dumpWindowInfo() {
        try {
            GLUtils.checkGLError("Pre-dumpWindowInfo");
            int r = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE);
            int g = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE);
            int b = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE);
            int a = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL11.GL_BACK_LEFT,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE);
            int depth = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE);
            int stencil = GL30.glGetFramebufferAttachmentParameteri(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL,
                    GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
            logger.info("Window Info: r=" + r + " g=" + g + " b=" + b + " a=" + a + " depth=" + depth + " stencil="
                    + stencil);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to dump window info", e);
        }
    }

    private void initNative(boolean crashed) throws Exception {
        this.audioManager = new OpenALManager(getSettings().audio.headphone_mode)
                .setSfxGain(getSettings().audio.sound_gain)
                .setMusicGain(getSettings().audio.music_gain)
                .setSfxEnabled(getSettings().audio.play_sfx);

        try {
            int bpp = 32;
            try {
                bpp = window.getDisplayMode().getBitsPerPixel();
            } catch (Exception _) {
                /* ignore */
            } // ignore if not created

            window.setTitle("Tribal Trouble");
            // Fullscreen handled in create

            SerializableDisplayMode target_mode;
            int width = crashed ? getSettings().window.view_width : getSettings().window.new_view_width;
            int height = crashed ? getSettings().window.view_height : getSettings().window.new_view_height;
            int freq = crashed ? getSettings().window.view_freq : getSettings().window.new_view_freq;

            if (width < SerializableDisplayMode.MIN_WIDTH || height < SerializableDisplayMode.MIN_HEIGHT) {
                try {
                    var modes = window.getFullscreenDisplayModes();
                    target_mode = modes.isEmpty()
                            ? new SerializableDisplayMode(SerializableDisplayMode.MIN_WIDTH,
                                    SerializableDisplayMode.MIN_HEIGHT, 32, 60)
                            : modes.getFirst();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to get available modes for default selection", e);
                    target_mode = new SerializableDisplayMode(SerializableDisplayMode.MIN_WIDTH,
                            SerializableDisplayMode.MIN_HEIGHT, 32, 60);
                }
            } else {
                target_mode = new SerializableDisplayMode(width, height, bpp, freq);
            }

            boolean fs = getSettings().window.fullscreen && (!getEventQueue().getDeterministic().isPlayback()
                    || grab_frames);
            window.create(target_mode, fs);
            setModeToNearest(target_mode);

            Path iconPath = Path.of("assets/widget/TribalTrouble.wdgt/Icon.png");
            if (!Files.exists(iconPath)) {
                iconPath = Path.of("../assets/widget/TribalTrouble.wdgt/Icon.png");
            }
            logger.info("Setting icon from: " + iconPath.toAbsolutePath());
            window.setIcon(iconPath);

            var physSize = window.getMonitorPhysicalSize();
            logger.info("Monitor Physical Size: " + (int) physSize.x() + "mm x " + (int) physSize.y() + "mm");
            var monScale = window.getMonitorContentScale();
            logger.info("Monitor Content Scale: " + monScale.x() + "x, " + monScale.y() + "y");
            var winScale = window.getWindowContentScale();
            logger.info("Window Content Scale: " + winScale.x() + "x, " + winScale.y() + "y");

//if (System.currentTimeMillis() > 0)
//throw new LWJGLException("It failed because you asked it to.");
        } catch (Exception e) {
            if (null != audioManager) {
                audioManager.close();
                audioManager = null;
            }
            failedOpenGL(e);
            throw e;
        }
        String version = GL11.glGetString(GL11.GL_VERSION);
        logger.info("GL version: '" + version + "'");
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        logger.info("GL vendor: '" + vendor + "'");
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        logger.info("GL renderer: '" + renderer + "'");

        renderContext.init();
        dumpWindowInfo();

        int num_combined_tex_units = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        logger.info("GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS: " + num_combined_tex_units);
        if (num_combined_tex_units < 8) {
            throw new IllegalStateException("Number of combined texture image units " + num_combined_tex_units
                    + " is less than the required 8.");
        }

        logger.info("vsync = " + getSettings().window.vsync);
        if (getSettings().window.vsync)
            window.setVSyncEnabled(true);
        initGL();
        initVisibleGL();

        startSound();
    }

    public void toggleSound() {
        getSettings().audio.play_sfx = !getSettings().audio.play_sfx;
        getAudioManager().setSfxEnabled(getSettings().audio.play_sfx);
    }

    public void toggleMusic() {
        getSettings().audio.play_music = !getSettings().audio.play_music;
        if (getSettings().audio.play_music) {
            initMusicPlayer();
        } else if (music != null) {
            music.stop(1.2f);
        }
    }

    private void initMusicPlayer() {
        assert null != music_audio && music_audio.audio().isStreaming() : "Inappropriate music file";
        music = getAudioManager().newAudio(0f, 0f, 0f, music_audio);
    }

    public void setMusic(@NonNull AudioParameters music_audio, float delay) {
        this.music_audio = music_audio;

        if (music != null && getSettings().audio.play_music) {
            music.stop(1.2f);
        }
        if (getSettings().audio.play_music) {
            if (music_timer != null)
                music_timer.stop();
            music_timer = new TimerAnimation(new MusicTimer(), delay);
            music_timer.start();
        }
    }

    private final class MusicTimer implements Updatable<TimerAnimation> {
        @Override
        public void update(@NonNull TimerAnimation anim) {
            if (music_timer != null)
                music_timer.stop();
            music_timer = null;
            if (getSettings().audio.play_music) {
                initMusicPlayer();
            }
        }
    }

    public AudioPlayer getMusicPlayer() {
        return music;
    }

    private void initVisibleGL() {
        if (window != null) window.update();
    }

    public static void initGL() {
        RenderContext context = getRenderer().renderContext;
        VBO.releaseAll(context);
        context.applyDefaults();
        // Sync viewport with actual window dimensions
        Window window = getRenderer().window;
        int w = window.getWidth();
        int h = window.getHeight();
        logger.info("[Renderer] initGL: window.getWidth()=" + w + ", window.getHeight()=" + h);
        context.setViewport(0, 0, w, h);
    }

    public static void clearScreen() {
        GL11.glClearColor(Color.Linear.BLACK.r(), Color.Linear.BLACK.g(), Color.Linear.BLACK.b(), Color.Linear.BLACK
                .a());
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }
}
