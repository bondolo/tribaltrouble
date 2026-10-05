package com.oddlabs.tt.gui;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Coordinator managing mutually exclusive selection across grouped radio buttons. */
public final class RadioButtonGroup {
    private final List<RadioButtonGroupElement> buttons = new ArrayList<>();

    public RadioButtonGroup() {
    }

    public void mark(RadioButtonGroupElement button) {
        RadioButtonGroupElement marked = getMarked();
        if (marked != null)
            marked.setMarked(false);
        button.setMarked(true);
    }

    public void add(RadioButtonGroupElement button) {
        buttons.add(button);
    }

    public @Nullable RadioButtonGroupElement getMarked() {
        for (RadioButtonGroupElement button : buttons) {
            if (button.isMarked())
                return button;
        }
        return null;
    }
}
