package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;

import java.util.Objects;

/**
 * Radio button control belonging to a mutually exclusive group.
 */
public final class RadioButton extends RadioButtonGroupElement {
    private final Skin skin;
    private boolean pressed = false;

    public RadioButton(GUIRoot guiRoot, boolean marked, RadioButtonGroup group, String text) {
        super(marked, group);
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        Label label = new Label(text, skin.getEditFont());
        label.setPos(skin.getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth(), (skin
                .getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getHeight() - label.getHeight()) / 2);
        addChild(label);
        setDim(skin.getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getWidth() + label.getWidth(), skin
                .getRadioButtonMarked().get(ModeIconQuads.Mode.NORMAL).getHeight());
        setCanFocus(true);
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    protected void mouseReleased(MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mousePressed(MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        // When unpressed, active, pressed, and hovered, it should show the marked state
        IconQuad quad_to_render = isMarked()
                ? skin.getRadioButtonMarked().quad(skinMode)
                : skinMode == ModeIconQuads.Mode.ACTIVE && pressed && isHovered()
                        ? skin.getRadioButtonMarked().quad(skinMode)
                : skin.getRadioButtonUnmarked().quad(skinMode);

        renderer.drawIcon(quad_to_render, 0, 0);
    }
}
