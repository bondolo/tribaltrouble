package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.base.geom.BoundingBox;
import com.oddlabs.tt.base.geom.BoundsProvider;

/**
 * Visual element that appears as a target indicator when the user clicks on the landscape.
 */
public final class LandscapeTargetRespond implements Animated, BoundsProvider {
    public static final int SIZE = 128;
    private static final float SECOND_PER_PICK_RESPOND = 1f / 3f;

    private final AnimationManager animation_manager;
    private final float x;
    private final float y;
    private final BoundingBox bounds = new BoundingBox();
    private final BoundingBox[] boundsArray = new BoundingBox[]{bounds};
    private float time;

    public LandscapeTargetRespond(AnimationManager animation_manager, float x, float y) {
        this.animation_manager = animation_manager;
        this.x = x;
        this.y = y;
        this.bounds.setBounds(x - SIZE / 2f, x + SIZE / 2f, y - SIZE / 2f, y + SIZE / 2f,
                Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        this.time = SECOND_PER_PICK_RESPOND;
        animation_manager.registerAnimation(this);
    }

    public float getPositionX() {
        return x;
    }

    public float getPositionY() {
        return y;
    }

    @Override
    public BoundingBox[] bounds() {
        return boundsArray;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    @Override
    public void animate(float t) {
        if (time > 0) {
            time = Math.max(0, time - t);
        } else {
            animation_manager.removeAnimation(this);
        }
    }

    public boolean isFinished() {
        return time <= 0;
    }

    public float getProgress() {
        return time / SECOND_PER_PICK_RESPOND;
    }
}
