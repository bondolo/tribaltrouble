package com.oddlabs.tt.base.util;

import com.oddlabs.util.Color;

/**
 * Interface for printing informational chat and command status messages.
 */
@FunctionalInterface
public interface InfoPrinter {
    void print(String text);

    default void print(String text, Color color) {
        print(text);
    }
}
