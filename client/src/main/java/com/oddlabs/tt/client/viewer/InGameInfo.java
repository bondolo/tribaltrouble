package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.client.camera.Camera;
import com.oddlabs.tt.client.delegate.GameStatsDelegate;
import com.oddlabs.tt.gui.Group;

/**
 * Interface defining lifecycle and GUI integration hooks for active game modes.
 */
public interface InGameInfo {
    void addGameOverGUI(WorldViewer viewer, GameStatsDelegate delegate, int header_y,
            Group buttons);

    default void openInGameMenu(WorldViewer viewer, Camera camera) {
    }

    void abort(WorldViewer viewer);

    void close(WorldViewer viewer);

    boolean isMultiplayer();

    boolean isRated();

    float getRandomStartPosition();

    /**
     * Whether loading hints should be shown on the progress form for this game mode.
     *
     * @return true if hints should be displayed, false otherwise
     */
    default boolean showLoadingHints() {
        return true;
    }
}
