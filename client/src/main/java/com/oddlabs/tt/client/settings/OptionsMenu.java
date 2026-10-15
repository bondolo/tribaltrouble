package com.oddlabs.tt.client.settings;

import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.gui.GUIRoot;

/**
 * Main options menu dialog for configuring graphics, audio, keybindings, accessibility, and language.
 */
public final class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(GUIRoot gui_root, Peer peer) {
        super(gui_root, peer);
        chooseGamespeed(getPreferredGamespeed());
    }
}
