package com.oddlabs.tt.core.util;

/**
 * Functional interface for progress reporting during map loading and generation.
 */
@FunctionalInterface
public interface ProgressListener {
    /**
     * Listener instance that ignores progress notifications.
     */
    ProgressListener NONE = step -> {
    };

    void onProgress(float step);

    default void onProgress() {
        onProgress(0f);
    }
}
