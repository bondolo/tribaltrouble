package com.oddlabs.tt.simulation.model;

/**
 * Marker interface for client-side presentation state bound directly to a simulation {@link Model}.
 */
public interface ClientModel extends AutoCloseable {
    @Override
    default void close() {
    }
}
