package com.oddlabs.tt.net;


import org.jspecify.annotations.Nullable;

/** Network pairing holding an optional server and client instance. */
public record GameNetwork(@Nullable Server server, Client client) {
    public void closeServer() {
        if (server != null)
            server.close();
    }

    public Client getClient() {
        return client;
    }

    public void close() {
        client.close();
        closeServer();
    }
}
