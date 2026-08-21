package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.ModeIconQuads;
import com.oddlabs.tt.engine.render.GUIRenderer;

public final class Horizontal {
    private final ModeIconQuads left;
    private final ModeIconQuads center;
    private final ModeIconQuads right;
    private final int height;
    private final int left_width;
    private final int right_width;

    public Horizontal(ModeIconQuads left, ModeIconQuads center, ModeIconQuads right) {
        this.left = left;
        this.center = center;
        this.right = right;
        height = left.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        left_width = left.quad(ModeIconQuads.Mode.NORMAL).getWidth();
        right_width = right.quad(ModeIconQuads.Mode.NORMAL).getWidth();
    }

    public void render(GUIRenderer renderer, float x, float y, int width,
            ModeIconQuads.Mode skinMode) {
        int center_width = width - left_width - right_width;

        renderer.drawModeIcon(left, skinMode, x, y);
        renderer.drawIcon(center.quad(skinMode), x + left_width, y, center_width, height);
        renderer.drawModeIcon(right, skinMode, x + left_width + center_width, y);
    }

    public int getHeight() {
        return height;
    }
}
