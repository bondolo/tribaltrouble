package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.GUIRenderer;

public class GUIIcon extends GUIObject {
    private final IconQuad icon;

    public GUIIcon(IconQuad icon) {
        this.icon = icon;
        setDim(icon.getWidth(), icon.getHeight());
        setCanFocus(false);
    }

    @Override
    public void renderGeometry(GUIRenderer renderer) {
        renderer.drawIcon(icon, 0, 0, getWidth(), getHeight());
    }
}
