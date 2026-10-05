package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.engine.render.GUIRenderer;

/**
 * Draggable vertical thumb button within a scrollbar track.
 */
final class ScrollButton extends GUIObject {
    private final ScrollBar owner;

    ScrollButton(ScrollBar owner) {
        this.owner = owner;
        setCanFocus(true);
    }

    public void setupPos(ScrollBar owner) {
        setPos(owner.getButtonX(), owner.getButtonY());
        setDim(owner.getScrollBarData().scrollButton().getWidth(), owner.getButtonHeight());
    }

    @Override
    public void handleInput(InputEvent event) {
        if (event.hasAction(GameAction.UI_FOCUS_NEXT)) {
            // Bubble Tab
            return;
        }
        // Swallow others
        event.consume();
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        var scrollBarData = owner.getScrollBarData();
        scrollBarData.scrollButton()
                .render(renderer, 0, 0, getHeight(), skinMode);
    }

    @Override
    public void mouseClicked(MouseButton button, int x, int y, int clicks) {
    }
}
