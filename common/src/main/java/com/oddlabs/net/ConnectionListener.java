package com.oddlabs.net;

import org.jspecify.annotations.Nullable;

/**
 * Listens for incoming network connections and manages their lifecycle.
 */
public interface ConnectionListener extends AutoCloseable {
    /**
     * Accepts an incoming connection and binds the given connection interface to it.
     *
     * @param connection_interface interface to receive connection lifecycle and message events
     * @return the accepted connection instance
     */
    AbstractConnection acceptConnection(@Nullable ConnectionInterface connection_interface);

    /**
     * Rejects the pending incoming connection.
     */
    void rejectConnection();

    /**
     * Closes this connection listener and releases underlying network resources.
     */
    @Override
    void close();
}
