package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;

public class ImageButton extends ButtonObject {
    private final GUIObject normal;
    private final GUIObject hovered;
    private final GUIObject disabled;

    public ImageButton(GUIObject normal, GUIObject hovered, GUIObject disabled) {
        super(Skin.getSkin().getEditFont());
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
