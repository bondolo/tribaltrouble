package com.oddlabs.tt.core.net;

import org.jspecify.annotations.NonNull;

/**
 * Interface for displaying player join, leave, and status notifications.
 */
@FunctionalInterface
public interface PlayerNotificationListener {
    void notify(@NonNull String message);
}
