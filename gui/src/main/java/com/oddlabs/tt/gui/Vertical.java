package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;

public final class Vertical {
    private final ModeIconQuads bottom;
    private final ModeIconQuads center;
    private final ModeIconQuads top;
    private final int bottom_height;
    private final int top_height;
    private final int width;

    public Vertical(ModeIconQuads bottom, ModeIconQuads center, ModeIconQuads top) {
        this.bottom = bottom;
        this.center = center;
        this.top = top;
        bottom_height = bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        top_height = top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        width = bottom.quad(ModeIconQuads.Mode.NORMAL).getWidth();
    }

    public void render(GUIRenderer renderer, float x, float y, int height,
            ModeIconQuads.Mode skinMode) {
        int center_height = height - bottom_height - top_height;

        renderer.drawModeIcon(bottom, skinMode, x, y);
        renderer.drawIcon(center.quad(skinMode), x, y + bottom_height, width, center_height);
        renderer.drawModeIcon(top, skinMode, x, y + bottom_height + center_height);
    }

    public int getWidth() {
        return width;
    }

    public int getMinHeight() {
        return bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight() + top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
    }
}
