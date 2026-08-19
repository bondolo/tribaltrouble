package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.gui.Group;
import org.jspecify.annotations.NonNull;

/**
 * Interface defining lifecycle and GUI integration hooks for active game modes.
 */
public interface InGameInfo {
    void addGameOverGUI(@NonNull WorldViewer viewer, @NonNull GameStatsDelegate delegate, int header_y,
            @NonNull Group buttons);

    default void openInGameMenu(@NonNull WorldViewer viewer, @NonNull Camera camera) {
    }

    void abort(@NonNull WorldViewer viewer);

    void close(@NonNull WorldViewer viewer);

    boolean isMultiplayer();

    boolean isRated();

    float getRandomStartPosition();
}
