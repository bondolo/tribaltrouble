package com.oddlabs.tt.effects.particle;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.util.Color;
import org.joml.Vector3f;

/**
 * A transient expanding-ring effect rendered by {@link com.oddlabs.tt.effects.render.SonicBlastRenderer}.
 * Advances through its animation each game tick and removes itself when its duration elapses.
 */
public final class SonicBlastEffect implements Animated {
    private final World world;
    private final Vector3f position;
    private final float maxRadius;
    private final float duration;
    private final Color.Linear color;
    private float time;
    private boolean dead;

    public SonicBlastEffect(World world, Vector3f position, float maxRadius, float duration) {
        this(world, position, maxRadius, duration, new Color.Linear(0.7f, 0.85f, 1.0f, 1.0f));
    }

    public SonicBlastEffect(World world, Vector3f position, float maxRadius, float duration,
            Color.Linear color) {
        this.world = world;
        this.position = position;
        this.maxRadius = maxRadius;
        this.duration = duration;
        this.color = color;
        this.time = 0;
        this.dead = false;

        world.getAnimationManagerGameTime().registerAnimation(this);
    }

    public float getPositionX() {
        return position.x;
    }

    public float getPositionY() {
        return position.y;
    }

    public float getPositionZ() {
        return position.z;
    }

    public Color.Linear getColor() {
        return color;
    }

    public void update(float dt) {
        time += dt;
        if (time >= duration) {
            dead = true;
            world.getAnimationManagerGameTime().removeAnimation(this);
        }
    }

    public void abort() {
        if (!dead) {
            dead = true;
            world.getAnimationManagerGameTime().removeAnimation(this);
        }
    }

    @Override
    public void animate(float t) {
        update(t);
    }

    public World getWorld() {
        return world;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getTime() {
        return time;
    }

    public float getMaxRadius() {
        return maxRadius;
    }

    public boolean isDead() {
        return dead;
    }

    public float getDuration() {
        return duration;
    }
}
