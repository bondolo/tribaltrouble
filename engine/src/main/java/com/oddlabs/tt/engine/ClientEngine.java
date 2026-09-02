package com.oddlabs.tt.engine;

import com.oddlabs.event.Deterministic;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.global.GamePaths;
import com.oddlabs.tt.engine.render.ClientStartup;
import com.oddlabs.tt.engine.render.DebugFlags;
import com.oddlabs.tt.engine.render.FrameDriver;
import com.oddlabs.tt.engine.render.FramePacer;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.engine.render.state.RenderContext;
import com.oddlabs.tt.engine.settings.Settings;
import com.oddlabs.tt.engine.util.GLUtils;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.window.LWJGL3Window;
import com.oddlabs.tt.window.SerializableDisplayMode;
import com.oddlabs.tt.window.Window;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Desktop game client application host and game loop coordinator.
 * Manages the main game loop, window event pumping, audio updates, simulation pacing, and display presentation.
 */
public final class ClientEngine implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(ClientEngine.class.getSimpleName());
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final boolean PROFILE = Boolean.getBoolean("com.oddlabs.tt.profile");

    /** This is expected to be initialized before Locale.setDefault is ever called */
    private static final Locale default_locale = Locale.of(Locale.getDefault().getLanguage(), Locale.getDefault()
            .getCountry(), "default");

    private static final int INSTRUMENTATION_FRAME_COUNT = Integer.MAX_VALUE;

    private volatile boolean finished = false;
    private static boolean grab_frames = false;

    private final GamePaths gamePaths;
    private final Settings settings;
    private final Window window;
    private final LocalEventQueue event_queue;
    private final Network network;
    private final AudioManager audioManager;
    private final FramePacer framePacer = new FramePacer();
    private final Renderer renderer;

    private boolean movie_recording_started = false;

    private int instrumentationFrameCounter;
    private long totalPollEventsTime;
    private long totalRunGameLoopTime;
    private long totalAudioUpdateTime;
    private long totalWindowUpdateTime;
    private long totalDisplayTime;
    private long totalGLFinishTime;
    private long totalLoopTime;

    public ClientEngine(
            GamePaths gamePaths,
            Settings settings,
            Window window,
            LocalEventQueue eventQueue,
            Network network,
            AudioManager audioManager
    ) {
        this.gamePaths = gamePaths;
        this.settings = settings;
        this.window = window;
        this.event_queue = eventQueue;
        this.network = network;
        this.audioManager = audioManager;
        window.setSettings(settings.window);
        this.renderer = new Renderer(window, settings);
    }

    public void shutdown() {
        finished = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public GamePaths getGamePaths() {
        return gamePaths;
    }

    public Settings getSettings() {
        return settings;
    }

    public Window getWindow() {
        return window;
    }

    public LocalEventQueue getEventQueue() {
        return event_queue;
    }

    public Network getNetwork() {
        return network;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public FramePacer getFramePacer() {
        return framePacer;
    }

    public void updateProgress(FrameDriver driver) {
        renderer.updateProgress(driver);
    }

    public float getFPS() {
        return renderer.getFPS();
    }

    public Locale getDefaultLocale() {
        return default_locale;
    }

    /** global sound enable */
    public void startSound() {
        audioManager.startSources();
    }

    /** global sound disable */
    public void stopSound() {
        audioManager.stopSources();
    }

    public void toggleSound() {
        settings.audio.play_sfx = !settings.audio.play_sfx;
        audioManager.setSfxEnabled(settings.audio.play_sfx);
    }

    public void startMovieRecording() {
        logger.info("ACTION! Movie recording started.");
        movie_recording_started = true;
    }

    public SerializableDisplayMode getCurrentDisplayMode() {
        return event_queue.getDeterministic()
                .log(window.getDisplayMode());
    }

    public void toggleFullscreen() {
        try {
            boolean fs = !window.isFullscreen() && !event_queue.getDeterministic().isPlayback();
            logger.info("Toggling fullscreen to: " + fs + ". Current mode: " + window.getDisplayMode());
            window.setFullscreen(fs);
            settings.window.fullscreen = fs;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Mode switching failed with exception", e);
            throw new IllegalStateException("Mode switching failed", e);
        }
    }

    public void switchMode(SerializableDisplayMode mode, boolean switch_now) {
        if (switch_now) {
            try {
                window.setDisplayMode(mode);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            modeSwitchedNow(mode);
        } else {
            modeSwitchedLater(mode);
        }
    }

    public void setModeToNearest(SerializableDisplayMode mode) {
        boolean fs = settings.window.fullscreen;
        window.create(mode, fs);
        modeSwitchedNow(mode);
    }

    private void modeSwitchedLater(SerializableDisplayMode new_mode) {
        settings.window.fullscreen = window.isFullscreen();
        settings.window.new_view_width = new_mode.getWidth();
        settings.window.new_view_height = new_mode.getHeight();
        settings.window.new_view_freq = new_mode.getFrequency();
    }

    private void modeSwitchedNow(SerializableDisplayMode new_mode) {
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

    private void runGameLoop(FrameDriver driver) {
        if (framePacer.isTimeFrozen() && !framePacer.isTimeStopped()) {
            framePacer.unfreezeTime();
        }
        long current_time;
        if (grab_frames) {
            framePacer.warpTime(settings.frame_grab_milliseconds_per_frame);
            current_time = framePacer.getSystemTime();
        } else {
            current_time = framePacer.getSystemTime();
        }
        long last_frame_time = framePacer.getLastFrameTime();
        long time_diff = current_time - last_frame_time;
        framePacer.setLastFrameTime(current_time);
        Deterministic deterministic = event_queue.getDeterministic();
        if (time_diff > AnimationManager.MAX_STEP_MILLIS && !deterministic.isPlayback()) {
            Logger.getLogger(ClientEngine.class.getName()).warning("Skipping large time diff: "
                    + time_diff + " ms.");
            time_diff = 0;
        }

        framePacer.getFrameTimeCounter().updateAbsolute(time_diff);
        framePacer.addExecutionTimePrecision(framePacer.getFrameTimeCounter().getAveragePerUpdate());
        deterministic.setEnabled(true);
        var selector = network.getSelector();
        while (framePacer.getExecutionTimePrecision()
                >= AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK && !isFinished()) {
            framePacer.addExecutionTimePrecision(
                    (float) -AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK);
            framePacer.addExecutionTime(AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK);
            event_queue.tickHighPrecision(AnimationManager.ANIMATION_SECONDS_PER_PRECISION_TICK);
            while (framePacer.getExecutionTime() >= AnimationManager.ANIMATION_MILLISECONDS_PER_TICK
                    && !isFinished()) {
                selector.tick();

                driver.tick(selector);
                if (deterministic.log(window.isOpen() && window.isCloseRequested())) {
                    window.setCloseRequested(false);
                    driver.onCloseRequested();
                }
                framePacer.pathfindsPerTick.updateAbsolute(
                        com.oddlabs.tt.simulation.pathfinder.PathFinder.stat_pathfinder_per_frame);
                com.oddlabs.tt.simulation.pathfinder.PathFinder.stat_pathfinder_per_frame = 0;
                event_queue.tickLowPrecision(AnimationManager.ANIMATION_SECONDS_PER_TICK);
                framePacer.addExecutionTime(-AnimationManager.ANIMATION_MILLISECONDS_PER_TICK);
                framePacer.addChecksumMillisecondCounter(AnimationManager.ANIMATION_MILLISECONDS_PER_TICK);
                if (framePacer.getChecksumMillisecondCounter()
                        >= AnimationManager.ANIMATION_MILLISECONDS_PER_CHECKSUM) {
                    framePacer.addChecksumMillisecondCounter(
                            -AnimationManager.ANIMATION_MILLISECONDS_PER_CHECKSUM);
                    int checksum = event_queue.computeChecksum();
                    int logged_checksum = deterministic.log(checksum);
                    if (checksum != logged_checksum && framePacer.shouldComplainChecksum()) {
                        Logger.getLogger(ClientEngine.class.getName()).severe(
                                "********** ERROR: Checksum mismatch at tick " + event_queue
                                        .getHighPrecisionManager().getTick() + " | checksum = " + checksum
                                        + " | logged_checksum = " + logged_checksum + " **********");
                        framePacer.setChecksumComplain(false);
                    }
                }
                if (!DebugFlags.frustum_freeze) {
                    driver.pickHover();
                }
            }
        }
        deterministic.setEnabled(false);
    }

    public void run(ClientStartup startup, String... args) throws IOException {
        Instant start_time = Instant.now();
        logger.info("********** Running tt **********");
        logger.info("game dir: " + gamePaths.dataDir());
        logger.info("logs dir: " + gamePaths.logDir());
        boolean eventload = false;
        boolean zipped = false;
        boolean silent = false;
        for (int i = 0; i < args.length; i++) {
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
        }

        settings.load();

        if (eventload || grab_frames) {
            Path last_event_log_path = settings.last_event_log_dir.resolve(zipped ? "event.log.gz" : "event.log");
            logger.info("last_event_log_path = " + last_event_log_path);
            event_queue.loadEvents(last_event_log_path, zipped);
        }

        Path event_logs_dir = gamePaths.logDir();
        Path event_log_dir = event_logs_dir.resolve(Long.toString(System.currentTimeMillis()));
        if (settings.save_event_log) {
            setupLogging(event_log_dir, silent);
            event_queue.setEventsLogged(event_log_dir.resolve(com.oddlabs.util.Utils.EVENT_LOG));
        }
        Deterministic deterministic = event_queue.getDeterministic();
        settings.setPlayback(deterministic.isPlayback());
        var game_dir = deterministic.log(gamePaths.dataDir());
        event_log_dir = deterministic.log(event_log_dir);
        deterministic.log(settings);
        Locale language = "default".equals(settings.control.language)
                ? deterministic.log(ClientEngine.default_locale) : Locale.forLanguageTag(settings.control.language);
        IO.println("Using language " + language);
        Locale.setDefault(language);

        Path last_event_log_dir = settings.last_event_log_dir;
        boolean crashed = settings.crashed;
        try {
            renderer.initNative(crashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed initializing natives", e);
        }

        if (!settings.inDeveloperMode() && !deterministic.isPlayback()) {
            deleteOldLogs(last_event_log_dir.toFile(), event_log_dir.toFile(), event_logs_dir.toFile());
        }

        Duration startup_time_init = Duration.between(start_time, Instant.now());
        logger.info("Init done after " + startup_time_init + "ms");

        startSound();

        ScopedValue.where(RenderContext.CURRENT, renderer.getRenderContext()).run(() -> {
            try {
                runSession(startup, start_time);
            } finally {
                cleanup();
            }
        });
    }

    private void runSession(ClientStartup startup, Instant startTime) {
        ClientStartup.Session session = startup.init(this, true);
        session.driver().run(() -> runMainLoop(session, startTime));
    }

    private void runMainLoop(ClientStartup.Session session, Instant startTime) {
        FrameDriver driver = session.driver();
        Runnable load_task = session.loadTask();
        boolean first_frame = true;
        boolean wasActive = window.isActive();
        while (!finished) {
            long frameStart = System.nanoTime();

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
                logger.info("[ClientEngine] Focus Gained (isActive=true, wasActive=" + wasActive + ")");
                if (window.isIconified()) {
                    window.restore();
                }
                if (!window.isVisible()) {
                    window.show();
                }
                window.focus();
            } else if (!isActive && wasActive) {
                logger.info("[ClientEngine] Focus Lost (isActive=false, wasActive=" + wasActive + ")");
                if (settings.window.fullscreen) {
                    window.minimize();
                }
            }
            wasActive = isActive;

            long t2 = System.nanoTime();
            runGameLoop(driver);
            long t3 = System.nanoTime();
            totalRunGameLoopTime += (t3 - t2);

            long t4 = System.nanoTime();
            audioManager.update(AnimationManager.ANIMATION_SECONDS_PER_TICK);
            long t5 = System.nanoTime();
            totalAudioUpdateTime += (t5 - t4);

            audioManager.setMasterGain(isActive ? 1f : 0f);
            long t6 = System.nanoTime();
            if (!first_frame && window.isVisible()) {
                window.update();
            }
            long t7 = System.nanoTime();
            totalWindowUpdateTime += (t7 - t6);

            long t8 = System.nanoTime();
            renderer.display(driver);
            long t9 = System.nanoTime();
            totalDisplayTime += (t9 - t8);

            if (PROFILE) {
                long tf0 = System.nanoTime();
                GL11.glFinish();
                long tf1 = System.nanoTime();
                totalGLFinishTime += (tf1 - tf0);
            }

            if (first_frame) {
                Duration startup_time = Duration.between(startTime, Instant.now());
                logger.info("First frame rendered after " + startup_time);
                first_frame = false;
                if (load_task != null) {
                    window.update();
                    event_queue.getDeterministic().setEnabled(true);
                    try {
                        load_task.run();
                    } finally {
                        event_queue.getDeterministic().setEnabled(false);
                        renderer.getRenderContext().reset();
                    }
                    load_task = null;
                }
            }
            if (grab_frames && movie_recording_started) {
                GLUtils.takeScreenshot("");
            }

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

        event_queue.getDeterministic().setEnabled(true);
        settings.save();
    }

    private void setupLogging(Path event_log_dir, boolean silent) throws IOException {
        try {
            Files.createDirectories(event_log_dir);
            logger.info("Writing log files in " + event_log_dir);

            Logger rootLogger = Logger.getLogger("");
            for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            FileHandler fileHandler = new FileHandler(event_log_dir.resolve("output.log").toString());
            fileHandler.setFormatter(new SimpleFormatter());
            rootLogger.addHandler(fileHandler);

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

    private static void deleteLog(Path log) throws IOException {
        for (Path LOG_FILES : com.oddlabs.util.Utils.LOG_FILES) {
            Path log_file = log.resolve(LOG_FILES);
            Files.deleteIfExists(log_file);
        }
        Files.deleteIfExists(log);
    }

    private static void deleteOldLogs(File last_log_dir, File new_log_dir, File logs_dir) {
        File[] logs = logs_dir.listFiles();
        if (logs == null) {
            return;
        }
        for (File log : logs) {
            try {
                if (!log.isDirectory() || log.equals(last_log_dir) || log.equals(new_log_dir)) {
                    continue;
                }
                deleteLog(log.toPath());
            } catch (IOException _) {
                /* ignore */
            }
        }
    }

    public void cleanup() {
        logger.info("Cleaning up engine...");
        renderer.cleanup();
        logger.info("Engine cleanup complete. Exiting");
    }

    @Override
    public void close() {
        cleanup();
    }
}
