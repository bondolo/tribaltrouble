package com.oddlabs.net;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Skeletal implementation of a {@link ConnectionListener} delegating lifecycle callbacks.
 *
 * @param <A> the type of address or connection identifier emitted on incoming connections
 */
public abstract class AbstractConnectionListener<A> implements ConnectionListener {
    private final ConnectionListenerInterface<? super A> listener_interface;

    protected AbstractConnectionListener(ConnectionListenerInterface<? super A> connection_listener) {
        this.listener_interface = connection_listener;
    }

    @Override
    public abstract void close();

    @Override
    public abstract AbstractConnection acceptConnection(@Nullable ConnectionInterface connection_interface);

    @Override
    public abstract void rejectConnection();

    protected final void notifyIncomingConnection(A address) {
        listener_interface.incomingConnection(this, address);
    }

    protected final void notifyError(IOException e) {
        close();
        listener_interface.error(this, e);
    }
}
