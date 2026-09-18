package com.oddlabs.net;

import java.io.IOException;

/**
 * Callback handler for events emitted by a {@link ConnectionListener}.
 *
 * @param <A> the type of address or connection identifier emitted on incoming connections
 */
public interface ConnectionListenerInterface<A> {
    void error(ConnectionListener listener, IOException e);

    void incomingConnection(ConnectionListener listener, A address);
}
