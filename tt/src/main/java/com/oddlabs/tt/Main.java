package com.oddlabs.tt;

import com.oddlabs.tt.audio.AudioProvider;
import com.oddlabs.tt.audio.AudioSettings;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.global.GamePaths;
import com.oddlabs.tt.input.InputBindingSettings;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.render.ClientStateInitializer;
import com.oddlabs.tt.content.form.QuitForm;
import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.engine.ClientEngine;
import com.oddlabs.tt.engine.render.ClientStartup;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.InputManager;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.window.LWJGL3Window;
import org.lwjgl.sdl.SDLMessageBox;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.sdl.SDLMessageBox.SDL_MESSAGEBOX_ERROR;

/**
 * Main application entry point for Tribal Trouble.
 */
public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());

    private static String i18n(String key, Object... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static void fail(Throwable t) {
        logger.log(Level.SEVERE, "Critical Failure", t);

        if (!DEBUG) {
            while (t.getCause() != null) {
                t = t.getCause();
            }
            String error = i18n("error");
            String error_msg;
            try {
                error_msg = i18n("error_message", t.toString());
            } catch (IllegalArgumentException e) {
                // Fallback if message formatting fails (e.g. quotes in the exception message)
                error_msg = "Error: " + t;
            }
            logger.log(Level.SEVERE, error + ": " + error_msg);
            SDLMessageBox.SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, error, error_msg,
                    org.lwjgl.system.MemoryUtil.NULL);
        }
    }

    public static void shutdown(int status) {
        logger.info("Exiting");
        System.exit(status);
    }

    static void main(String... args) {
        logger.info("DEBUG mode = " + DEBUG);
        String os_name = System.getProperty("os.name");
        logger.info("os.name = '" + os_name + "'");
        String os_arch = System.getProperty("os.arch");
        logger.info("os.arch = '" + os_arch + "'");
        String os_version = System.getProperty("os.version");
        logger.info("os.version = '" + os_version + "'");
        String java_version = System.getProperty("java.version");
        logger.info("java.version = '" + java_version + "'");
        String java_vendor = System.getProperty("java.vendor");
        logger.info("java.vendor = '" + java_vendor + "'");
        long total_mem = Runtime.getRuntime().maxMemory();
        logger.info("maxMemory = '" + total_mem + "'");

        int status = 1;
        try {
            logger.info("Starting game....");
            GamePaths gamePaths = new GamePaths();
            Settings settings = new Settings(gamePaths.dataDir());
            AudioSettings audioSettings = AudioSettings.from(settings);
            try (var window = new LWJGL3Window(); var eventQueue = new LocalEventQueue(); var audioManager
                    = AudioProvider.load(audioSettings, eventQueue.getManager())) {
                audioManager.setSfxGain(audioSettings.sound_gain)
                        .setMusicGain(audioSettings.music_gain)
                        .setSfxEnabled(audioSettings.play_sfx);

                Network network = new Network(new com.oddlabs.net.NetworkSelector(eventQueue.getDeterministic(),
                        eventQueue::getMillis));
                ClientEngine engine = new ClientEngine(gamePaths, settings, window, eventQueue, network, audioManager);
                engine.run(
                        (clientEngine, firstProgress) -> {
                            ClientStateInitializer.init(audioManager);
                            InputManager inputManager = new InputManager(InputBindingSettings.from(settings));
                            LocalInput localInput = new LocalInput(
                                    clientEngine.getWindow(), inputManager,
                                    eventQueue.getDeterministic(), () -> clientEngine.getSettings().inDeveloperMode(),
                                    clientEngine::shutdown,
                                    clientEngine.getFramePacer()
                            );
                            Menu.initNetwork(clientEngine);
                            localInput.init();
                            settings.last_event_log_dir = gamePaths.logDir().toAbsolutePath();
                            settings.crashed = true;
                            settings.save();
                            settings.crashed = false;
                            GUI gui = new GUI(
                                    localInput,
                                    clientEngine.getWindow(),
                                    eventQueue,
                                    settings,
                                    clientEngine::shutdown,
                                    clientEngine::updateProgress,
                                    () -> clientEngine.getFPS()
                            );
                            gui.setMovieRecordingStarter(clientEngine::startMovieRecording);
                            gui.setCloseHandler(() -> {
                                if (gui.getGUIRoot().isShowingModalForm(QuitForm.class)) {
                                    clientEngine.shutdown();
                                } else {
                                    gui.getGUIRoot().addModalForm(new QuitForm(
                                            clientEngine::shutdown));
                                }
                            });
                            Runnable loadTask = gui.callWithSkin(() -> Menu.setupMainMenu(
                                    clientEngine,
                                    gui,
                                    firstProgress
                            ));
                            return new ClientStartup.Session(gui, loadTask);
                        }, args
                );
            }
            status = 0;
        } catch (Throwable t) {
            fail(t);
        } finally {
            shutdown(status);
        }
    }
}
