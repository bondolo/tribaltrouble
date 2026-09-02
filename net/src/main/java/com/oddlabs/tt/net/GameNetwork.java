package com.oddlabs.tt.net;


/** Network pairing holding a server and client instance. */
public final class GameNetwork<C, R> {
    private final Server server;
    private final Client<C, R> client;

    public GameNetwork(Server server, Client<C, R> client) {
        this.server = server;
        this.client = client;
        assert client != null;
    }

    public void closeServer() {
        if (server != null)
            server.close();
    }

    public Client<C, R> getClient() {
        return client;
    }

    public void close() {
        client.close();
        closeServer();
    }
}
