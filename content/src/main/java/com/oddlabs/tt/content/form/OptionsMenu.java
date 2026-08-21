package com.oddlabs.tt.content.form;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;

/**
 * Main options menu dialog for configuring graphics, audio, keybindings, accessibility, and language.
 */
public final class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(@NonNull GUIRoot gui_root, @NonNull AudioManager audioManager) {
        super(gui_root, audioManager);
        chooseGamespeed(getPreferredGamespeed());
    }
}
