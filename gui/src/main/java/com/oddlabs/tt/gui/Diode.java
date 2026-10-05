package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;

import java.util.Objects;

/**
 * Status indicator diode light.
 */
public final class Diode extends GUIObject {
    private final Skin skin;
    private boolean lit;

    public Diode(GUIRoot guiRoot) {
        this.skin = Objects.requireNonNull(guiRoot.getSkin(), "Skin cannot be null");
        var normal = skin.getDiode().get(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
        lit = false;
    }

    @Override
    protected Skin getSkin() {
        return skin;
    }

    public void setLit(boolean lit) {
        this.lit = lit;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled()
                ? ModeIconQuads.Mode.DISABLED
                : lit
                        ? ModeIconQuads.Mode.ACTIVE
                : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(skin.getDiode(), skinMode, 0, 0);
    }
}
