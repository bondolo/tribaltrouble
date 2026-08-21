package com.oddlabs.tt.content.form;

import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.gui.event.*;
import com.oddlabs.tt.client.gui.*;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.simulation.landscape.WorldGenerator;

/**
 * Callback interface for multiplayer lobby forms to receive notification when a game connection completes.
 */
public interface MultiplayerLobby {
    void createGameMenu(GameNetwork game_network, Game game,
            WorldGenerator generator, int player_slot);
}
