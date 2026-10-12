package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/**
 * Text box container rendered against a styled background panel.
 */
public final class BackgroundLabelBox extends LabelBox {
    private final Skin skin;

    public BackgroundLabelBox(GUIRoot guiRoot, CharSequence text, Font font, int width) {
        super(text, font, width);
        this.skin = guiRoot.getSkin();
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    @Override
    protected final void renderGeometry(GUIRenderer renderer) {
        Box background_box = skin.getBackgroundBox();
        background_box.render(renderer, 0f, 1f, getWidth(), getHeight() - 2, ModeIconQuads.Mode.NORMAL);
        super.renderGeometry(renderer);
    }
}
