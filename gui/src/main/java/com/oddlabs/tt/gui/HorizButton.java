package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;

import java.util.Objects;

/**
 * Push button rendered with horizontal cap and fill quads.
 */
public class HorizButton extends ButtonObject {
    private final Skin skin;

    public HorizButton(GUIRoot guiRoot, String caption, int width) {
        super(guiRoot.getSkin().getButtonFont());
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        setDim(width, skin.getHorizButtonPressed().getHeight());
        Label label = new Label(caption, getFont());
        label.setPos((width - label.getWidth()) / 2,
                (skin.getHorizButtonPressed().getHeight() - getFont().getHeight()) / 2);
        addChild(label);
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    protected final void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : isPressed() && isHovered()
                        ? ModeIconQuads.Mode.ACTIVE
                : isActive()
                        ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        var horizButton = skinMode == ModeIconQuads.Mode.ACTIVE && isPressed() && isHovered()
                ? skin.getHorizButtonPressed()
                : skin.getHorizButtonUnpressed();

        horizButton.render(renderer, 0, 0, getWidth(), skinMode);
    }
}
