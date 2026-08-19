package com.oddlabs.tt.client.viewer;

import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.animation.TimerAnimation;
import com.oddlabs.tt.base.animation.Updatable;
import com.oddlabs.tt.audio.AudioImplementation;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.client.gui.Arrow;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.simulation.landscape.HeightMap;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * A temporary, animated on-screen alert directing the player's attention
 * to a specific location in the game world, typically accompanied by a sound.
 */
public class Notification implements Updatable<TimerAnimation> {

    private static final float ACTIVE_SECONDS = 5f;

    private final float center_x;
    private final float center_y;
    private final @NonNull NotificationManager manager;
    private final @NonNull TimerAnimation timer;
    private final @NonNull Arrow arrow;

    public Notification(@NonNull HeightMap heightMap, @NonNull AudioImplementation audio, @NonNull GUIRoot gui_root,
            float x, float y, @NonNull NotificationManager manager, @NonNull Color color,
            @NonNull AudioParameters params, boolean show_always,
            @NonNull AnimationManager animation_manager) {
        this.center_x = x;
        this.center_y = y;
        this.manager = manager;
        this.timer = new TimerAnimation(animation_manager, this, ACTIVE_SECONDS);
        timer.start();
        this.arrow = new Arrow(heightMap, gui_root, center_x, center_y, color, show_always);
        gui_root.addChild(arrow);
        audio.newAudio(0f, 0f, 0f, params);
    }

    public void remove() {
        arrow.remove();
        timer.stop();
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        remove();
        manager.removeNotification(this);
    }

    protected final @NonNull Arrow getArrow() {
        return arrow;
    }

    protected final @NonNull TimerAnimation getTimer() {
        return timer;
    }

    protected final @NonNull NotificationManager getManager() {
        return manager;
    }

    public final float getX() {
        return center_x;
    }

    public final float getY() {
        return center_y;
    }
}
