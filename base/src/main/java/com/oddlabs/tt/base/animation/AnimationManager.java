package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/// Manages simulation tick progression and tick animation listener callbacks.
public final class AnimationManager {
    private static final Logger logger = Logger.getLogger(AnimationManager.class.getName());
    public static final long ANIMATION_MILLISECONDS_PER_TICK = 20L;
    public static final long ANIMATION_MILLISECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_TICK / 5;
    public static final float ANIMATION_SECONDS_PER_TICK = ANIMATION_MILLISECONDS_PER_TICK / 1000f;
    public static final float ANIMATION_SECONDS_PER_PRECISION_TICK = ANIMATION_MILLISECONDS_PER_PRECISION_TICK / 1000f;
    public static final long ANIMATION_MILLISECONDS_PER_CHECKSUM = TimeUnit.SECONDS.toMillis(2);
    public static final long MAX_STEP_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private final List<@Nullable Animated> animations = new ArrayList<>();

    private int tick;
    private boolean runningAnimations;
    private boolean hasDeletions;

    public int getTick() {
        return tick;
    }

    public void registerAnimation(Animated anim) {
        Objects.requireNonNull(anim, "anim must not be null");
        if (!animations.contains(anim)) {
            animations.add(anim);
        }
    }

    public void removeAnimation(@Nullable Animated anim) {
        if (anim != null) {
            int idx = animations.indexOf(anim);
            if (idx >= 0) {
                if (runningAnimations) {
                    animations.set(idx, null);
                    hasDeletions = true;
                } else {
                    animations.remove(idx);
                }
            }
        }
    }

    public void updateChecksum(StateChecksum checksum) {
        for (int i = 0; i < animations.size(); i++) {
            Animated anim = animations.get(i);
            if (anim != null) {
                anim.updateChecksum(checksum);
            }
        }
    }

    public void runAnimations(float dt) {
        tick++;
        runningAnimations = true;
        try {
            for (int i = 0; i < animations.size(); i++) {
                Animated anim = animations.get(i);
                if (anim != null) {
                    anim.animate(dt);
                }
            }
        } finally {
            runningAnimations = false;
            if (hasDeletions) {
                animations.removeIf(Objects::isNull);
                hasDeletions = false;
            }
        }
    }

    public void debugPrintAnimations() {
        for (int i = 0; i < animations.size(); i++) {
            Animated anim = animations.get(i);
            if (anim != null) {
                logger.fine("anim = " + anim);
            }
        }
    }
}
