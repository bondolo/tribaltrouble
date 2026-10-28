package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Unit tests for {@link AnimationManager}.
final class AnimationManagerTest {

    @Test
    void testTickIncrementAndAnimationDispatch() {
        AnimationManager manager = new AnimationManager();
        assertEquals(0, manager.getTick());

        AtomicInteger callCount = new AtomicInteger(0);
        Animated anim = _ -> callCount.incrementAndGet();

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

    @Test
    void testDuplicateRegistrationIgnored() {
        AnimationManager manager = new AnimationManager();
        AtomicInteger count = new AtomicInteger(0);
        Animated anim = _ -> count.incrementAndGet();

        manager.registerAnimation(anim);
        manager.registerAnimation(anim);
        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);

        assertEquals(1, count.get(), "Duplicate registration must only dispatch once per tick");
    }

    @Test
    void testRegistrationDuringTickExecutesInSameTick() {
        AnimationManager manager = new AnimationManager();
        List<String> executionLog = new ArrayList<>();

        Animated childAnim = _ -> executionLog.add("child");
        Animated parentAnim = _ -> {
            executionLog.add("parent");
            manager.registerAnimation(childAnim);
        };

        manager.registerAnimation(parentAnim);
        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);

        assertEquals(List.of("parent", "child"), executionLog,
                "Child animation registered during tick must execute in same tick");
    }

    @Test
    void testCrossRemovalDuringTickPreventsExecutionInSameTick() {
        AnimationManager manager = new AnimationManager();
        AtomicBoolean targetExecuted = new AtomicBoolean(false);
        Animated targetAnim = _ -> targetExecuted.set(true);

        Animated removerAnim = _ -> manager.removeAnimation(targetAnim);

        manager.registerAnimation(removerAnim);
        manager.registerAnimation(targetAnim);

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);

        assertFalse(targetExecuted.get(), "Animation removed before its turn in the same tick must not execute");
    }

    @Test
    void testRemovalAndReAdditionInSameTick() {
        AnimationManager manager = new AnimationManager();
        AtomicInteger count = new AtomicInteger(0);
        Animated anim = _ -> count.incrementAndGet();

        manager.registerAnimation(anim);
        manager.removeAnimation(anim);
        manager.registerAnimation(anim);

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(1, count.get(), "Re-registered animation must remain active");
    }

    @Test
    void testPreservesDeterministicExecutionOrderAcrossCompaction() {
        AnimationManager manager = new AnimationManager();
        List<String> executed = new ArrayList<>();

        Animated a = _ -> executed.add("A");
        Animated b = _ -> executed.add("B");
        Animated c = _ -> executed.add("C");
        Animated d = _ -> executed.add("D");
        Animated e = _ -> executed.add("E");

        manager.registerAnimation(a);
        manager.registerAnimation(b);
        manager.registerAnimation(c);
        manager.registerAnimation(d);
        manager.registerAnimation(e);

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(List.of("A", "B", "C", "D", "E"), executed);

        executed.clear();
        manager.removeAnimation(b);
        manager.removeAnimation(d);

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(List.of("A", "C", "E"), executed,
                "Relative order of non-deleted animations must be strictly preserved");
    }

    @Test
    void testUpdateChecksumDispatchesToAllActive() {
        AnimationManager manager = new AnimationManager();
        List<String> checksumLog = new ArrayList<>();

        class ChecksumTrackingAnim implements Animated {
            private final String name;

            ChecksumTrackingAnim(String name) {
                this.name = name;
            }

            @Override
            public void animate(float dt) {
            }

            @Override
            public void updateChecksum(StateChecksum checksum) {
                checksumLog.add(name);
                checksum.update(name.hashCode());
            }
        }

        manager.registerAnimation(new ChecksumTrackingAnim("first"));
        ChecksumTrackingAnim second = new ChecksumTrackingAnim("second");
        manager.registerAnimation(second);
        manager.registerAnimation(new ChecksumTrackingAnim("third"));

        manager.removeAnimation(second);

        StateChecksum checksum = new StateChecksum();
        manager.updateChecksum(checksum);

        assertEquals(List.of("first", "third"), checksumLog,
                "Checksum updates must reach only surviving active animations");
    }

    @Test
    void testLargeBatchScalingAndCompaction() {
        AnimationManager manager = new AnimationManager();
        int total = 200;
        AtomicInteger[] counters = new AtomicInteger[total];
        Animated[] anims = new Animated[total];

        for (int i = 0; i < total; i++) {
            counters[i] = new AtomicInteger(0);
            int idx = i;
            anims[i] = _ -> counters[idx].incrementAndGet();
            manager.registerAnimation(anims[i]);
        }

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        for (int i = 0; i < total; i++) {
            assertEquals(1, counters[i].get());
        }

        // Remove all even index animations
        for (int i = 0; i < total; i += 2) {
            manager.removeAnimation(anims[i]);
        }

        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        for (int i = 0; i < total; i++) {
            int expected = (i % 2 == 0) ? 1 : 2;
            assertEquals(expected, counters[i].get(), "Even animations should not increment after removal");
        }
    }

    @Test
    void testRemoveNonExistentOrNullDoesNotThrow() {
        AnimationManager manager = new AnimationManager();
        manager.removeAnimation(null);
        Animated unregistered = _ -> {
        };
        manager.removeAnimation(unregistered);
        manager.runAnimations(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        assertEquals(1, manager.getTick());
    }
}
