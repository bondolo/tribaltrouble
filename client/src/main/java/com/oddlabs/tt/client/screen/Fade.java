package com.oddlabs.tt.client.screen;

import com.oddlabs.tt.base.event.StateChecksum;
import com.oddlabs.tt.engine.render.GUIRenderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.Nullable;

/**
 * Handles full-screen fade transitions between screens.
 */
final class Fade {
    private static final float FADE_TIME = 1f;

    private final @Nullable Runnable onComplete;
    private final Screen screen;

    private float time = 0;
    private boolean image_switched = false;

    Fade(@Nullable Runnable onComplete, Screen screen) {
        this.onComplete = onComplete;
        this.screen = screen;
    }

    public Screen getScreen() {
        return screen;
    }

    public void animate(ScreenManager screenManager, float t) {
        time += t;
        if (!image_switched && time >= FADE_TIME / 2) {
            image_switched = true;
            screenManager.switchScreen(screen);
        }

        if (time >= FADE_TIME) {
            screenManager.stopFade();
            if (onComplete != null)
                onComplete.run();
        }
    }

    public void updateChecksum(StateChecksum checksum) {
    }

    void render(GUIRenderer guiRenderer, int width, int height) {
        var alpha = Math.clamp((float) Math.sin(Math.PI * time / FADE_TIME), 0f, 1f);
        guiRenderer.drawColoredQuad(0, 0, width, height, new Color.Linear(0f, 0f, 0f, alpha));
    }
}
