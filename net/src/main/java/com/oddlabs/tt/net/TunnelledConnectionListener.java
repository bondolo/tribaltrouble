package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.TunnelAddress;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.AbstractConnectionListener;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.ConnectionListenerInterface;
import com.oddlabs.net.HostSequenceID;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class TunnelledConnectionListener extends AbstractConnectionListener {
    private final Deque<TunnelledConnection> incoming_connections = new ArrayDeque<>();
    private final MatchmakingClient matchmaking_client;
    private boolean open = true;

    public TunnelledConnectionListener(MatchmakingClient matchmaking_client,
            ConnectionListenerInterface listener_interface) {
        super(listener_interface);
        this.matchmaking_client = matchmaking_client;
        matchmaking_client.registerTunnelledListener(this);
    }

    public void requestTunnelledConnection(HostSequenceID address, InetAddress inet_address,
            InetAddress local_address, Profile profile) {
        TunnelledConnection conn = new TunnelledConnection(matchmaking_client, address, null);
        incoming_connections.add(conn);
        notifyIncomingConnection(new TunnelIdentifier(profile, new TunnelAddress(address.getHostID(), inet_address,
                local_address)));
    }

    private TunnelledConnection getNextTunnel() {
        return incoming_connections.remove();
    }

    @Override
    protected AbstractConnection doAcceptConnection(@Nullable ConnectionInterface connection_interface) {
        TunnelledConnection conn = getNextTunnel();
        conn.setConnectionInterface(connection_interface);
        conn.accept();
        return conn;
    }

    @Override
    public void rejectConnection() {
        getNextTunnel().close();
    }

    public void connectionClosed() {
        open = false;
        notifyError(new ClosedChannelException());
    }

    @Override
    public void close() {
        if (open) {
            matchmaking_client.unregisterTunnelledListener(this);
            open = false;
        }
    }
}
