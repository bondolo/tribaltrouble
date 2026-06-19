package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

/**
 * Client-side animation representing the expanding sonic blast ring.
 */
public final class ClientSonicBlast implements Animated {
    private final float x;
    private final float y;
    private final float z;
    private final float maxRadius;
    private final float duration;
    private final Color.@NonNull Linear color;
    private final @NonNull RenderQueues queues;
    private final @NonNull AnimationManager manager;
    private float time;

    public ClientSonicBlast(
            float x, float y, float z,
            float maxRadius, float duration,
            Color.@NonNull Linear color,
            @NonNull RenderQueues queues, @NonNull AnimationManager manager) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.color = color;
        this.queues = queues;
        this.manager = manager;
        this.time = 0f;

        queues.addSonicBlast(this);
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

    public float getMaxRadius() {
        return maxRadius;
    }

    public float getDuration() {
        return duration;
    }

    public Color.@NonNull Linear getColor() {
        return color;
    }

    public float getTime() {
        return time;
    }

    @Override
    public void animate(float t) {
        time = Math.min(time + t, duration);
        if (time >= duration) {
            queues.removeSonicBlast(this);
            manager.removeAnimation(this);
        }
    }
}
