package com.oddlabs.tt.client.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.core.net.GameNetwork;
import com.oddlabs.tt.core.world.WorldGenerator;
import org.jspecify.annotations.NonNull;

/**
 * Callback interface for multiplayer lobby forms to receive notification when a game connection completes.
 */
public interface MultiplayerLobby {
    void createGameMenu(@NonNull GameNetwork game_network, @NonNull Game game,
            @NonNull WorldGenerator generator, int player_slot);
}
