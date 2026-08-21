package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.engine.render.ModeIconQuads;

/**
 * A 9-patch box definition for GUI background and border rendering.
 */
public final class Box {
    private final ModeIconQuads left_bottom;
    private final ModeIconQuads bottom;
    private final ModeIconQuads right_bottom;
    private final ModeIconQuads right;
    private final ModeIconQuads right_top;
    private final ModeIconQuads top;
    private final ModeIconQuads left_top;
    private final ModeIconQuads left;
    private final ModeIconQuads center;
    private final int left_offset;
    private final int bottom_offset;
    private final int right_offset;
    private final int top_offset;

    private final int left_width;
    private final int right_width;
    private final int bottom_height;
    private final int top_height;

    public Box(ModeIconQuads left_bottom,
            ModeIconQuads bottom,
            ModeIconQuads right_bottom,
            ModeIconQuads right,
            ModeIconQuads right_top,
            ModeIconQuads top,
            ModeIconQuads left_top,
            ModeIconQuads left,
            ModeIconQuads center,
            int left_offset,
            int bottom_offset,
            int right_offset,
            int top_offset) {
        this.left_bottom = left_bottom;
        this.bottom = bottom;
        this.right_bottom = right_bottom;
        this.right = right;
        this.right_top = right_top;
        this.top = top;
        this.left_top = left_top;
        this.left = left;
        this.center = center;
        this.left_offset = left_offset;
        this.bottom_offset = bottom_offset;
        this.right_offset = right_offset;
        this.top_offset = top_offset;

        left_width = left.quad(ModeIconQuads.Mode.NORMAL).getWidth();
        right_width = right.quad(ModeIconQuads.Mode.NORMAL).getWidth();
        bottom_height = bottom.quad(ModeIconQuads.Mode.NORMAL).getHeight();
        top_height = top.quad(ModeIconQuads.Mode.NORMAL).getHeight();
    }

    public void render(GUIRenderer renderer, float x, float y, int width, int height,
            ModeIconQuads.Mode skinMode) {
        int center_width = width - left_width - right_width;
        int center_height = height - bottom_height - top_height;

        renderer.drawModeIcon(left_bottom, skinMode, x, y);
        renderer.drawIcon(bottom.quad(skinMode), x + left_width, y, center_width, bottom_height);
        renderer.drawModeIcon(right_bottom, skinMode, x + left_width + center_width, y);
        renderer.drawIcon(right.quad(skinMode), x + left_width + center_width, y + bottom_height, right_width,
                center_height);
        renderer.drawModeIcon(right_top, skinMode, x + left_width + center_width, y + bottom_height + center_height);
        renderer.drawIcon(top.quad(skinMode), x + left_width, y + bottom_height + center_height, center_width,
                top_height);
        renderer.drawModeIcon(left_top, skinMode, x, y + bottom_height + center_height);
        renderer.drawIcon(left.quad(skinMode), x, y + bottom_height, left_width, center_height);
        renderer.drawIcon(center.quad(skinMode), x + left_width, y + bottom_height, center_width, center_height);
    }

    public int getLeftOffset() {
        return left_offset;
    }

    public int getBottomOffset() {
        return bottom_offset;
    }

    public int getRightOffset() {
        return right_offset;
    }

    public int getTopOffset() {
        return top_offset;
    }

    public ModeIconQuads getLeftBottom() {
        return left_bottom;
    }

    public ModeIconQuads getBottom() {
        return bottom;
    }

    public ModeIconQuads getRightBottom() {
        return right_bottom;
    }

    public ModeIconQuads getRight() {
        return right;
    }

    public ModeIconQuads getRightTop() {
        return right_top;
    }

    public ModeIconQuads getTop() {
        return top;
    }

    public ModeIconQuads getLeftTop() {
        return left_top;
    }

    public ModeIconQuads getLeft() {
        return left;
    }

    public ModeIconQuads getCenter() {
        return center;
    }

    public int getLeftWidth() {
        return left_width;
    }

    public int getRightWidth() {
        return right_width;
    }

    public int getBottomHeight() {
        return bottom_height;
    }

    public int getTopHeight() {
        return top_height;
    }
}
