package com.oddlabs.tt.gui;

import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.tt.gui.render.UIRenderer;
import com.oddlabs.tt.base.event.StateChecksum;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Handles full-screen fade transitions between different GUI states or root nodes.
 */
final class Fade {
    private static final float FADE_TIME = 1f;

    private final @Nullable Fadable fadable;
    private final GUIRoot gui_root;
    private final @Nullable UIRenderer renderer;

    private float time = 0;
    private boolean image_switched = false;

    Fade(@Nullable Fadable fadable, GUIRoot gui_root, @Nullable UIRenderer renderer) {
        this.fadable = fadable;
        this.gui_root = gui_root;
        this.renderer = renderer;
    }

    public void animate(GUI gui, float t) {
        time += t;
        if (!image_switched && time >= FADE_TIME / 2) {
            image_switched = true;
            gui.switchRoot(gui_root, renderer);
        }

        if (time >= FADE_TIME) {
            gui.stopFade();
            if (fadable != null)
                fadable.fadingDone();
        }
    }

    public void updateChecksum(StateChecksum checksum) {
    }

    void render(GUIRenderer guiRenderer) {
        var alpha = (float) Math.sin(Math.PI * time / FADE_TIME);
        guiRenderer.drawColoredQuad(0, 0, gui_root.getWidth(), gui_root.getHeight(), new Color.Linear(0f, 0f, 0f,
                alpha));
    }
}
