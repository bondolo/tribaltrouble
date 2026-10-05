package com.oddlabs.tt.gui.render;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;

/**
 * Renders a blinking text insertion cursor (caret) for text input fields.
 */
public final class Index {
    public static final int INDEX_WIDTH = 1;
    public static final long BLINK_INTERVAL_MS = 500L;

    private Index() {
    }

    public static void renderIndex(GUIRenderer renderer, int render_x, int render_y, Font font,
            Color.Linear color) {
        renderer.drawColoredQuad(render_x, render_y + 3, INDEX_WIDTH, font.getHeight() - 6, color);
    }
}
