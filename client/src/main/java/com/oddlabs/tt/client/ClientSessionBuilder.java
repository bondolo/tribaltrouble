package com.oddlabs.tt.client;

import com.oddlabs.tt.input.InputBindingSettings;
import com.oddlabs.tt.base.global.Settings;
import com.oddlabs.tt.client.gui.QuitForm;
import com.oddlabs.tt.client.resource.AudioRegistry;
import com.oddlabs.tt.client.screen.ScreenManager;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.InputManager;
import org.jspecify.annotations.Nullable;

/**
 * Builds and initializes client subsystems for an active session.
 */
public final class ClientSessionBuilder {

    /**
     * Initializes the initial screen and returns an optional background load task.
     */
    @FunctionalInterface
    public interface ScreenInitializer {
        @Nullable
        Runnable initialize(Peer peer, boolean firstProgress, ScreenManager screenManager);
    }

    private ClientSessionBuilder() {
    }

    public static ClientStartup create(ScreenInitializer initializer) {
        return (peer, firstProgress) -> build(peer, firstProgress, initializer);
    }

    private static ClientStartup.Session build(Peer peer, boolean firstProgress, ScreenInitializer initializer) {
        InputManager inputManager = new InputManager(InputBindingSettings.from(peer.getSettings()));
        LocalInput localInput = new LocalInput(
                peer.getWindow(), inputManager,
                peer.getEventQueue().getDeterministic(),
                peer::shutdown,
                peer.getFramePacer()
        );
        localInput.init();

        Settings settings = peer.getSettings();

        GUI gui = new GUI(
                localInput,
                peer.getWindow(),
                peer.getEventQueue(),
                settings,
                peer::shutdown,
                peer::updateProgress,
                peer::getFPS
        );
        gui.setMovieRecordingStarter(peer::startMovieRecording);
        gui.setErrorAudioHandler(() -> peer.getAudioManager().newAudio(0f, 0f, 0f,
                AudioRegistry.ERROR_SOUND));
        gui.setCloseHandler(() -> {
            if (gui.getGUIRoot().isShowingModalForm(QuitForm.class)) {
                peer.shutdown();
            } else {
                gui.getGUIRoot().addModalForm(new QuitForm(gui.getGUIRoot(),
                        peer::shutdown));
            }
        });

        ScreenManager screenManager = new ScreenManager(gui, peer.getEventQueue().getManager(),
                peer::updateProgress);
        Runnable loadTask = initializer.initialize(peer, firstProgress, screenManager);
        return new ClientStartup.Session(screenManager, loadTask);
    }
}
