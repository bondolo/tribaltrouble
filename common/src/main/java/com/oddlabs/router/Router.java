package com.oddlabs.router;

import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionListener;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.SocketConnectionListener;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Multiplexes client connections, manages game sessions, and routes lockstep messages.
 */
public final class Router implements ConnectionListenerInterface<InetAddress>, AutoCloseable {
    private final Logger logger;
    private final SessionManager manager;
    private final ConnectionListener listener;
    private final Set<RouterClient> clients = new LinkedHashSet<>();
    private final @Nullable RouterListener router_listener;
    private final int port;

    public Router(NetworkSelector network, Logger logger) {
        this(network, null, RouterInterface.PORT, logger, null);
    }

    public Router(NetworkSelector network, @Nullable InetAddress address, int port, Logger logger,
            @Nullable RouterListener router_listener) {
        this.manager = new SessionManager(network.getTimeManager(), logger);
        this.logger = logger;
        this.router_listener = router_listener;
        SocketConnectionListener tmp_listener = new SocketConnectionListener(network,
                new InetSocketAddress(address, port), this);
        this.listener = tmp_listener;
        this.port = tmp_listener.getPort();
    }

    public int getPort() {
        return port;
    }

    public long getNextTimeout() {
        return manager.getNextTimeout();
    }

    public void process() {
        manager.process();
    }

    @Override
    public void incomingConnection(ConnectionListener connection_listener, InetAddress remote_address) {
        logger.log(Level.INFO, "Incoming connection from {0}", remote_address);
        AbstractConnection conn = connection_listener.acceptConnection(null);
        RouterClient client = new RouterClient(manager, conn, logger, this);
        clients.add(client);
        conn.setConnectionInterface(client);
    }

    void removeClient(RouterClient client) {
        clients.remove(client);
    }

    @Override
    public void error(ConnectionListener conn_id, IOException e) {
        close();
        logger.log(Level.SEVERE, "Server socket failed: {0}", e);
        if (router_listener != null)
            router_listener.routerFailed(e);
    }

    @Override
    public void close() {
        if (listener != null)
            listener.close();
        Iterator<RouterClient> it = clients.iterator();
        while (it.hasNext()) {
            RouterClient client = it.next();
            it.remove();
            client.close(false);
        }
    }
}
