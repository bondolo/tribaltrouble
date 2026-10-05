package com.oddlabs.tt.gui;


/**
 * Composite group that ignores focus acquisition and mouse press events.
 */
public final class NonFocusGroup extends Group {
    public NonFocusGroup(GUIRoot guiRoot) {
        super(guiRoot, true);
        setTabStop(false);
    }

    @Override
    public final void setFocus() {
    }

    @Override
    public final void setFocus(FocusDirection direction) {
    }

    @Override
    public final void mousePressed(MouseButton button, int x, int y) {
    }
}
