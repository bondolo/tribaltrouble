package com.oddlabs.tt.content.form;

import com.oddlabs.tt.client.Peer;
import com.oddlabs.tt.gui.GUIRoot;

/**
 * Main options menu dialog for configuring graphics, audio, keybindings, accessibility, and language.
 */
public final class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(GUIRoot gui_root, Peer engine) {
        super(gui_root, engine);
        chooseGamespeed(getPreferredGamespeed());
    }
}
