package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;

/**
 * A user interface element that changes over time
 */
@FunctionalInterface
public interface Animated {
    void animate(float t);

    default void updateChecksum(StateChecksum checksum) {
    }
}
