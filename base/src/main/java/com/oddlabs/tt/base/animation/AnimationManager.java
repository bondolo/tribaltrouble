package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Manages simulation tick progression and tick animation listener callbacks.
 */
public final class AnimationManager {
    private static final Logger logger = Logger.getLogger(AnimationManager.class.getName());
    public static final long ANIMATION_MILLISECONDS_PER_TICK = 20L;
    public static final long ANIMATION_MILLISECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_TICK / 5;
    public static final float ANIMATION_SECONDS_PER_TICK = ANIMATION_MILLISECONDS_PER_TICK / 1000f;
    public static final float ANIMATION_SECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_PRECISION_TICK / 1000f;
    public static final long ANIMATION_MILLISECONDS_PER_CHECKSUM = TimeUnit.SECONDS.toMillis(2);
    public static final long MAX_STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final Set<Animated> animations = new CopyOnWriteArraySet<>();
    private final Set<Animated> deleted_animations = new CopyOnWriteArraySet<>();

    private int tick;

    public int getTick() {
        return tick;
    }

    public void registerAnimation(Animated anim) {
        deleted_animations.remove(anim);
        animations.add(anim);
    }

    public void removeAnimation(Animated anim) {
        if (animations.contains(anim)) {
            deleted_animations.add(anim);
        }
    }

    private void flushAnimations() {
        animations.removeAll(deleted_animations);
        deleted_animations.clear();
    }

    public void updateChecksum(StateChecksum checksum) {
        flushAnimations();
        animations.forEach(anim -> anim.updateChecksum(checksum));
    }

    public void runAnimations(float t) {
        tick++;
        flushAnimations();
        Predicate<Animated> notDeleted = ((Predicate<Animated>) deleted_animations::contains).negate();
        animations.stream()
                .filter(notDeleted)
                .forEach(a -> a.animate(t));
    }

    public void debugPrintAnimations() {
        flushAnimations();
        animations.forEach(anim -> logger.fine("anim = " + anim));
    }
}
