package com.oddlabs.tt.content.menu;

import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.gui.Group;

/**
 * Hook allowing game content modes to inject custom GUI widgets into the in-game main menu.
 */
@FunctionalInterface
public interface InGameMenuHook {
    /**
     * Adds custom GUI components to the in-game main menu.
     *
     * @param viewer the active world viewer
     * @param menu the in-game main menu
     * @param game_infos the group container for game information widgets
     */
    void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos);
}
