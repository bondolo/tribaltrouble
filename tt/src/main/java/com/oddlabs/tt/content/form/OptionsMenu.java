package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.tt.engine.Globals;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;

public final class OptionsMenu extends AbstractOptionsMenu {
    public OptionsMenu(@NonNull GUIRoot gui_root) {
        super(gui_root);
        chooseGamespeed(Globals.gamespeed);
    }
}
