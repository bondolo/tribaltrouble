package com.oddlabs.tt.client.gui;

/** A callback to be executed when a {@link Fade} is completed. */
@FunctionalInterface
public interface Fadable extends Runnable {
    void fadingDone();

    @Override
    default void run() {
        fadingDone();
    }
}
