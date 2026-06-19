package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import org.jspecify.annotations.NonNull;

/**
 * Client-side animation representing a click target response indicator.
 */
public final class ActiveTargetRespond implements Animated {
    private static final float SECONDS_PER_PICK_RESPOND = 1f / 3f;

    private final float x;
    private final float y;
    private final float z;
    private final @NonNull TargetRespondRenderer renderer;
    private final @NonNull AnimationManager manager;
    private float time;

    public ActiveTargetRespond(float x, float y, float z, @NonNull TargetRespondRenderer renderer,
            @NonNull AnimationManager manager) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.renderer = renderer;
        this.manager = manager;
        this.time = SECONDS_PER_PICK_RESPOND;
        renderer.addRespond(this);
        manager.registerAnimation(this);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getProgress() {
        return time / SECONDS_PER_PICK_RESPOND;
    }

    @Override
    public void animate(float t) {
        time = Math.max(0f, time - t);
        if (time <= 0f) {
            renderer.removeRespond(this);
            manager.removeAnimation(this);
        }
    }
}
