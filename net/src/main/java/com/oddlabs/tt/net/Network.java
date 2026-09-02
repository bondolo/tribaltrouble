package com.oddlabs.tt.net;


import com.oddlabs.net.NetworkSelector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Network {
    private final NetworkSelector selector;
    private final ChatHub chat_hub = new ChatHub();
    private final MatchmakingClient matchmaking_client = new MatchmakingClient(this);
    private @Nullable MatchmakingListener matchmaking_listener;

    public Network(@NonNull NetworkSelector selector) {
        this.selector = selector;
    }

    public @NonNull NetworkSelector getSelector() {
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

    public void closeMatchmakingClient() {
        matchmaking_listener = null;
        matchmaking_client.close();
    }
}
