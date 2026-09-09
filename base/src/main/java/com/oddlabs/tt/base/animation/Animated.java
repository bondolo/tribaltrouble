package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;

/**
 * A user interface element that changes over time
 */
@FunctionalInterface
public interface Animated {
    /**
     * animation tick
     *
     * @param dt elapsed time since the previous tick.
     */
    void animate(float dt);

    default void updateChecksum(StateChecksum checksum) {
    }
}
