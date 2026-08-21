package com.oddlabs.tt.gui.render;

import com.oddlabs.tt.engine.font.Font;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Renders a blinking text insertion cursor (caret) for text input fields.
 */
public final class Index {
    public static final int INDEX_WIDTH = 1;
    private static final long BLINK_INTERVAL_MS = 500L;

    private static long last_reset_time = System.currentTimeMillis();

    private Index() {
    }

    public static void resetBlinking() {
        last_reset_time = System.currentTimeMillis();
    }

    public static void renderIndex(@NonNull GUIRenderer renderer, int render_x, int render_y, @NonNull Font font,
            Color.@NonNull Linear color) {
        long elapsed = System.currentTimeMillis() - last_reset_time;
        boolean blink_on = (elapsed / BLINK_INTERVAL_MS) % 2 == 0;
        if (blink_on) {
            renderer.drawColoredQuad(render_x, render_y + 3, INDEX_WIDTH, font.getHeight() - 6, color);
        }
    }
}
