package com.oddlabs.tt.net;


/**
 * Interface for displaying player join, leave, and status notifications.
 */
@FunctionalInterface
public interface PlayerNotificationListener {
    void notify(String message);
}
