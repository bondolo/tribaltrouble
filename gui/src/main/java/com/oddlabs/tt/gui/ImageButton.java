package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;

/**
 * Button rendering stateful normal, hovered, and disabled graphical components.
 */
public final class ImageButton extends ButtonObject {
    private final GUIObject normal;
    private final GUIObject hovered;
    private final GUIObject disabled;

    public ImageButton(GUIRoot guiRoot, GUIObject normal, GUIObject hovered, GUIObject disabled) {
        this(guiRoot.getSkin().getEditFont(), normal, hovered, disabled);
    }

    public ImageButton(Font font, GUIObject normal, GUIObject hovered, GUIObject disabled) {
        super(font);
        setDim(normal.getWidth(), normal.getHeight());
        this.normal = normal;
        this.hovered = hovered;
        this.disabled = disabled;
    }

    @Override
    public final void setPos(int x, int y) {
        super.setPos(x, y);
        normal.setPos(x, y);
        hovered.setPos(x, y);
        disabled.setPos(x, y);
    }

    @Override
    protected final void renderGeometry(GUIRenderer renderer) {
        var render = isDisabled()
                ? disabled
                : isHovered() || isActive() ? hovered : normal;
        render.renderGeometry(renderer);
    }

    @Override
    protected void mouseClicked(MouseButton button, int x, int y, int clicks) {
    }
}
