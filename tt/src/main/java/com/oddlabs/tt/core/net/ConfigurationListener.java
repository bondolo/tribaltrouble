package com.oddlabs.tt.core.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.engine.resource.WorldGenerator;

public interface ConfigurationListener extends ErrorListener {
    void connected(Client client, Game game, WorldGenerator generator, int player_slot);

    void setPlayers(PlayerSlot[] players);

    void gameStarted();
}
