package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Non-focusable icon button used for HUD and action panel controls.
 */
public class NonFocusIconButton extends IconButton {
    public NonFocusIconButton(GUIRoot guiRoot, ModeIconQuads icon, @Nullable GameAction action, @Nullable Supplier<
            String> tool_tip) {
        super(guiRoot, icon, action, tool_tip);
        setTabStop(false);
    }

    @Override
    public final void setFocus() {
        // we don't want to be focused
    }

    @Override
    public final void setFocus(FocusDirection direction) {
        // we don't want to be focused
    }
}
