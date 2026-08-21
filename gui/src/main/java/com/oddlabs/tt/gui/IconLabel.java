package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.IconQuad;
import com.oddlabs.tt.engine.render.GUIRenderer;

public final class IconLabel extends GUIObject implements Comparable<IconLabel> {
    private final IconQuad icon;
    private final Label label;

    public IconLabel(IconQuad icon, Label label) {
        this.icon = icon;
        this.label = label;
        label.setPos(icon.getWidth(), 0);
        addChild(label);
        int width = icon.getWidth() + label.getWidth();
        int height = Math.max(icon.getHeight(), label.getHeight());
        setDim(width, height);
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        renderer.drawIcon(icon, 0, 0);
    }

    private Label getLabel() {
        return label;
    }

    @Override
    public int compareTo(IconLabel o) {
        return label.compareTo(o.getLabel());
    }
}
