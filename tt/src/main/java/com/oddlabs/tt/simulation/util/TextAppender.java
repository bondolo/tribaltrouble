package com.oddlabs.tt.simulation.util;

/**
 * Interface for appending formatted text lines or fragments to text buffers or debug UI widgets.
 */
@FunctionalInterface
public interface TextAppender {
    /**
     * Appends the string representation of an object to this buffer.
     *
     * @param obj The object or string to append.
     */
    void append(Object obj);
}
