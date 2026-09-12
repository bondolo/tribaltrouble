package com.oddlabs.matchservlet;

import io.micronaut.runtime.Micronaut;

/**
 * Entry point for the Tribal Trouble web services application.
 */
public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
