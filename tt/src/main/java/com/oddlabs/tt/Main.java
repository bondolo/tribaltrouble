package com.oddlabs.tt;

import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.base.event.LocalEventQueue;
import com.oddlabs.tt.base.global.GamePaths;
import com.oddlabs.tt.base.util.Utils;
import com.oddlabs.tt.client.render.ClientStateInitializer;
import com.oddlabs.tt.content.form.QuitForm;
import com.oddlabs.tt.content.menu.Menu;
import com.oddlabs.tt.engine.render.ClientStartup;
import com.oddlabs.tt.engine.render.Renderer;
import com.oddlabs.tt.engine.settings.Settings;
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
            long window = org.lwjgl.system.MemoryUtil.NULL;
            try {
                Renderer renderer = Renderer.getRenderer();
                if (renderer != null) {
                    window = ((LWJGL3Window) renderer.getWindow()).getHandle();
                }
            } catch (Exception e) {
                // Window might not be created yet, ignore
            }
            SDLMessageBox.SDL_ShowSimpleMessageBox(SDL_MESSAGEBOX_ERROR, error, error_msg, window);
        }
    }

    public static void shutdown(int status) {
        Renderer renderer = Renderer.getRenderer();
        if (renderer != null) {
            renderer.close();
        }
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
            Network network = new Network();
            try (var window = new LWJGL3Window(); var eventQueue = new LocalEventQueue(); var audioManager
                    = new OpenALManager(settings.audio, eventQueue.getManager())) {
                audioManager.setSfxGain(settings.audio.sound_gain)
                        .setMusicGain(settings.audio.music_gain)
                        .setSfxEnabled(settings.audio.play_sfx);

                Renderer renderer = new Renderer(gamePaths, settings, window, eventQueue, network, audioManager);
                renderer.run(
                        (net, firstProgress) -> {
                            ClientStateInitializer.init(audioManager);
                            InputManager inputManager = new InputManager(settings.inputBindings, settings.control);
                            LocalInput localInput = new LocalInput(
                                    renderer.getWindow(), inputManager,
                                    eventQueue.getDeterministic(), () -> renderer.getSettings().inDeveloperMode(),
                                    Renderer::shutdown
                            );
                            Menu.initNetwork(net);
                            localInput.init();
                            settings.last_event_log_dir = gamePaths.logDir().toAbsolutePath();
                            settings.crashed = true;
                            settings.save();
                            settings.crashed = false;
                            GUI gui = new GUI(localInput, eventQueue);
                            gui.setCloseHandler(() -> {
                                if (gui.getGUIRoot().isShowingModalForm(QuitForm.class)) {
                                    Renderer.shutdown();
                                } else {
                                    gui.getGUIRoot().addModalForm(new QuitForm(gui.getGUIRoot()));
                                }
                            });
                            Runnable loadTask = gui.callWithSkin(() -> Menu.setupMainMenu(
                                    net, gui, audioManager,
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
