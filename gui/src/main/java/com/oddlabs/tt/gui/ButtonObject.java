package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.input.GameAction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** A clickable button */
public abstract class ButtonObject extends GUIObject {
    private boolean pressed = false;
    private final Font font;
    private final @Nullable GameAction action;

    public ButtonObject(Font font) {
        this(font, null);
    }

    public ButtonObject(Font font, @Nullable GameAction action) {
        this(font, action, null);
    }

    public ButtonObject(Font font, @Nullable GameAction action, @Nullable Supplier<String> tool_tip) {
        super(tool_tip);
        this.font = font;
        this.action = action;
        setCanFocus(true);
    }

    protected Font getFont() {
        return font;
    }

    public final @Nullable GameAction getAction() {
        return action;
    }

    final boolean isPressed() {
        return pressed;
    }

    @Override
    protected final void mouseReleased(MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected final void mousePressed(MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void mouseHeld(MouseButton button, int x, int y) {
        if (pressed)
            mousePressedAll(button, x, y);
    }

    @Override
    public void setDisabled(boolean disabled) {
        if (disabled)
            pressed = false;
        super.setDisabled(disabled);
    }
}
