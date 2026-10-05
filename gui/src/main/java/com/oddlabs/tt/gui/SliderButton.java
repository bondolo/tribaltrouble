package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.engine.render.GUIRenderer;

import java.util.Objects;

/**
 * Draggable thumb button within a slider control track.
 */
final class SliderButton extends ButtonObject {
    private final Skin skin;
    private final Slider slider;
    private final ModeIconQuads button;

    SliderButton(Slider slider, ModeIconQuads button) {
        super(slider.getSkin().getEditFont());
        this.skin = Objects.requireNonNull(slider.getSkin(), "Skin cannot be null");
        this.slider = slider;
        this.button = button;
        setDim(button.quad(ModeIconQuads.Mode.NORMAL).getWidth(), button.quad(ModeIconQuads.Mode.NORMAL).getHeight());
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        GUIObject parent = getParent();
        ModeIconQuads.Mode skinMode = parent.isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : (isHovered() || parent.isActive())
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(button, skinMode, 0, 0);
    }

    @Override
    public void mouseHeld(MouseButton button, int x, int y) {
    }

    @Override
    public void handleInput(InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_NAV_RIGHT)) {
                slider.setValue(slider.getValue() + 1);
                return;
            }
            if (event.consumeAction(GameAction.UI_NAV_LEFT)) {
                slider.setValue(slider.getValue() - 1);
                return;
            }
        }

        // Swallow others (legacy behavior)
        event.consume();
    }
}
