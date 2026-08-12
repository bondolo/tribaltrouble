package com.oddlabs.tt.core.util;

import org.jspecify.annotations.NonNull;

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

    final class Holder {
        private static @NonNull ProgressListener activeListener = NONE;
    }

    static void setListener(@NonNull ProgressListener listener) {
        Holder.activeListener = listener;
    }

    static @NonNull ProgressListener getListener() {
        return Holder.activeListener;
    }

    static void progress() {
        Holder.activeListener.onProgress();
    }

    static void progress(float step) {
        Holder.activeListener.onProgress(step);
    }

    void onProgress(float step);

    default void onProgress() {
        onProgress(0f);
    }
}
