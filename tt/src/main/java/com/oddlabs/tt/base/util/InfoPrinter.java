package com.oddlabs.tt.base.util;

import org.jspecify.annotations.NonNull;

/**
 * Interface for printing informational chat and command status messages.
 */
@FunctionalInterface
public interface InfoPrinter {
    void print(@NonNull String text);
}
