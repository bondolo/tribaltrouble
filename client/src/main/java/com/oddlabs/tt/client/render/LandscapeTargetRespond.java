package com.oddlabs.tt.client.render;

import com.oddlabs.tt.base.animation.Animated;
import com.oddlabs.tt.base.animation.AnimationManager;
import com.oddlabs.tt.simulation.landscape.World;
import com.oddlabs.tt.simulation.model.Element;


/**
 * Visual element that appears as a target indicator when the user clicks on the landscape.
 */
public final class LandscapeTargetRespond extends Element<LandscapeTargetRespond> implements Animated {
    public static final int SIZE = 128;
    private static final float SECOND_PER_PICK_RESPOND = 1f / 3f;

    private final AnimationManager animation_manager;
    private float time;

    public LandscapeTargetRespond(World world, AnimationManager animation_manager, float x, float y) {
        super(world.getElementRoot());
        this.animation_manager = animation_manager;
        setPosition(x, y);
        setPositionZ(world.getHeightMap().getNearestHeight(x, y));
        setBounds(x - SIZE / 2, x + SIZE / 2, y - SIZE / 2, y + SIZE / 2, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        register();
    }

    @Override
    protected LandscapeTargetRespond self() {
        return this;
    }

    @Override
    public void animate(float t) {
        if (time > 0) {
            time = Math.max(0, time - t);
        } else {
            remove();
        }
    }

    public float getProgress() {
        return time / SECOND_PER_PICK_RESPOND;
    }

    @Override
    public void register() {
        super.register();
        time = SECOND_PER_PICK_RESPOND;
        animation_manager.registerAnimation(this);
    }

    @Override
    public void remove() {
        super.remove();
        animation_manager.removeAnimation(this);
    }
}
