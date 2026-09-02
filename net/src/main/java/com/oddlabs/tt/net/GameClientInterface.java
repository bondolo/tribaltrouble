package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

/** ARMI RPC interface for client-bound game setup commands. */
public interface GameClientInterface {
    void setWorldGeneratorAndPlayerSlot(Game game, WorldGenerator<?> generator,
            short player_slot);

    void setPlayers(PlayerSlot[] players);

    void startGame(int session_id);

    void chat(int player_slot, String chat);
}
