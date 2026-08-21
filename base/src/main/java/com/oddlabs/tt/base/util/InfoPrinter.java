package com.oddlabs.tt.base.util;


/**
 * Interface for printing informational chat and command status messages.
 */
@FunctionalInterface
public interface InfoPrinter {
    void print(String text);
}
