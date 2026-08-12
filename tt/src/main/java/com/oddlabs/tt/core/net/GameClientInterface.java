package com.oddlabs.tt.core.net;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.player.PlayerSlot;
import com.oddlabs.tt.core.world.WorldGenerator;
import org.jspecify.annotations.NonNull;

/** ARMI RPC interface for client-bound game setup commands. */
public interface GameClientInterface {
    void setWorldGeneratorAndPlayerSlot(@NonNull Game game, @NonNull WorldGenerator generator,
            short player_slot);

    void setPlayers(PlayerSlot[] players);

    void startGame(int session_id);

    void chat(int player_slot, String chat);
}
