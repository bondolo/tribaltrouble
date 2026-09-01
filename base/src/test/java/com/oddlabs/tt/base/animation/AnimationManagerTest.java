package com.oddlabs.tt.base.animation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link AnimationManager}.
 */
class AnimationManagerTest {

    @Test
    void testTickIncrementAndAnimationDispatch() {
        AnimationManager manager = new AnimationManager();
        assertEquals(0, manager.getTick());

        AtomicInteger callCount = new AtomicInteger(0);
        Animated anim = t -> callCount.incrementAndGet();

        manager.registerAnimation(anim);
        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);

        assertEquals(1, manager.getTick());
        assertEquals(1, callCount.get());

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(2, manager.getTick());
        assertEquals(2, callCount.get());

        manager.removeAnimation(anim);
        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(3, manager.getTick());
        assertEquals(2, callCount.get());
    }

    @Test
    void testMultipleIndependentManagers() {
        AnimationManager manager1 = new AnimationManager();
        AnimationManager manager2 = new AnimationManager();

        manager1.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        manager1.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);

        assertEquals(2, manager1.getTick());
        assertEquals(0, manager2.getTick());
    }
}
