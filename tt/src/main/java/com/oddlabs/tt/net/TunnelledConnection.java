package com.oddlabs.tt.net;

import com.oddlabs.net.ARMIEvent;
import com.oddlabs.net.AbstractConnection;
import com.oddlabs.net.ConnectionInterface;
import com.oddlabs.net.HostSequenceID;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public final class TunnelledConnection extends AbstractConnection {
    private final @NonNull MatchmakingClient matchmaking_client;
    private final HostSequenceID address;
    private boolean open = true;

    public TunnelledConnection(@NonNull MatchmakingClient matchmaking_client, HostSequenceID address,
            ConnectionInterface conn_interface) {
        this.matchmaking_client = matchmaking_client;
        setConnectionInterface(conn_interface);
        this.address = address;
        matchmaking_client.registerTunnel(this.address, this);
        notifyConnected();
    }

    public TunnelledConnection(@NonNull MatchmakingClient matchmaking_client, int address,
            ConnectionInterface conn_interface) {
        this.matchmaking_client = matchmaking_client;
        setConnectionInterface(conn_interface);
        this.address = matchmaking_client.registerTunnel(address, this);
    }

    public void tunnelClosed() {
        open = false;
        notifyError(new IOException("Connection closed"));
    }

    public void connected() {
        notifyConnected();
    }

    public void accept() {
        matchmaking_client.getInterface().acceptTunnel(address);
    }

    @Override
    public void handle(ARMIEvent event) {
        matchmaking_client.getInterface().routeEvent(address, event);
        writeBufferDrained();
    }

    public HostSequenceID getAddress() {
        return address;
    }

    @Override
    protected void doClose() {
        if (open) {
            matchmaking_client.unregisterTunnel(address, this);
            open = false;
        }
    }
}
