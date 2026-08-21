package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.base.util.LoadCallback;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

/** Listener interface receiving game lobby configuration events. */
public interface ConfigurationListener extends ErrorListener {
    void connected(Client client, Game game, WorldGenerator generator, int player_slot);

    void setPlayers(PlayerSlot[] players);

    void gameStarted(LoadCallback<?, ?> loadCallback);
}
