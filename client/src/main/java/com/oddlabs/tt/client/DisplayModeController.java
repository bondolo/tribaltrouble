package com.oddlabs.tt.client;

import com.oddlabs.tt.window.SerializableDisplayMode;

/**
 * Controller for toggling fullscreen and switching window/display resolution modes.
 */
public interface DisplayModeController {
    /**
     * Toggles fullscreen state.
     */
    void toggleFullscreen();

    /**
     * Switches display mode either immediately or scheduled for next restart.
     *
     * @param mode the target display mode
     * @param switchNow true to switch immediately, false to defer
     */
    void switchMode(SerializableDisplayMode mode, boolean switchNow);
}
