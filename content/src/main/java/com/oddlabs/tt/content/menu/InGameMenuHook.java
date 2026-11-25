package com.oddlabs.tt.content.menu;

import com.oddlabs.tt.client.viewer.WorldViewer;
import com.oddlabs.tt.gui.Group;

/// Hook allowing game content modes to inject custom GUI widgets into the in-game main menu.
@FunctionalInterface
public interface InGameMenuHook {
  void addGUI(WorldViewer viewer, InGameMainMenu menu, Group game_infos);
}
