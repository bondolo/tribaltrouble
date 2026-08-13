package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.core.util.LoadCallback;
import com.oddlabs.tt.engine.resource.WorldGenerator;
import org.jspecify.annotations.NonNull;

/** Listener interface receiving game lobby configuration events. */
public interface ConfigurationListener extends ErrorListener {
    void connected(@NonNull Client client, @NonNull Game game, @NonNull WorldGenerator generator, int player_slot);

    void setPlayers(PlayerSlot[] players);

    void gameStarted(@NonNull LoadCallback<?, ?> loadCallback);
}
