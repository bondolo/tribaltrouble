package com.oddlabs.tt.client.gui;

import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.NonFocusIconButton;
import com.oddlabs.tt.gui.ToolTipBox;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Non-focusable icon button associated with an IconSpinner. */
public final class IconSpinnerButton extends NonFocusIconButton {
    private final IconSpinner owner;

    public IconSpinnerButton(GUIRoot guiRoot, ModeIconQuads icon_quad, @Nullable GameAction action, Supplier<
            String> tool_tip,
            IconSpinner owner) {
        super(guiRoot, icon_quad, action, tool_tip);
        this.owner = owner;
    }

    @Override
    public boolean hasToolTip() {
        return isDisabled() ? owner.hasToolTip() : super.hasToolTip();
    }

    @Override
    public void appendToolTip(ToolTipBox tool_tip_box) {
        if (isDisabled())
            owner.appendToolTip(tool_tip_box);
        else
            super.appendToolTip(tool_tip_box);
    }
}
