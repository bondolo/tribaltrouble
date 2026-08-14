package com.oddlabs.tt.base.animation;

import com.oddlabs.tt.base.event.StateChecksum;
import org.jspecify.annotations.NonNull;

/**
 * A user interface element that changes over time
 */
@FunctionalInterface
public interface Animated {
    void animate(float t);

    default void updateChecksum(@NonNull StateChecksum checksum) {
    }
}
