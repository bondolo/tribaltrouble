package com.oddlabs.tt.net;


import com.oddlabs.net.NetworkSelector;
import org.jspecify.annotations.Nullable;

/**
 * Top-level network coordinator managing selector ticking, matchmaking, and chat.
 */
public final class Network implements AutoCloseable {
    private final NetworkSelector selector;
    private final ChatHub chat_hub = new ChatHub();
    private final MatchmakingClient matchmaking_client = new MatchmakingClient(this);
    private @Nullable MatchmakingListener matchmaking_listener;

    public Network(NetworkSelector selector) {
        this.selector = selector;
    }

    public NetworkSelector getSelector() {
        return selector;
    }

    public @Nullable MatchmakingListener getMatchmakingListener() {
        return matchmaking_listener;
    }

    public void setMatchmakingListener(@Nullable MatchmakingListener listener) {
        matchmaking_listener = listener;
    }

    public ChatHub getChatHub() {
        return chat_hub;
    }

    public MatchmakingClient getMatchmakingClient() {
        return matchmaking_client;
    }

    @Override
    public void close() {
        matchmaking_listener = null;
        matchmaking_client.close();
        selector.close();
    }
}
