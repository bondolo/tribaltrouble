package com.oddlabs.tt.core.animation;

import com.oddlabs.tt.core.event.StateChecksum;
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
