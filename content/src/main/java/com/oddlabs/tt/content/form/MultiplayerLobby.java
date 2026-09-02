package com.oddlabs.tt.content.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.render.UIRenderer;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

/**
 * Multiplayer lobby form callback to receive notification when a game connection completes.
 */
public interface MultiplayerLobby {
    void createGameMenu(GameNetwork<GUIRoot, UIRenderer> game_network, Game game,
            WorldGenerator<?> generator, int player_slot);
}
