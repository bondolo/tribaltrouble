package com.oddlabs.tt.content.skirmish;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.simulation.landscape.IslandConfig;
import com.oddlabs.tt.simulation.landscape.WorldParameters;
import com.oddlabs.tt.simulation.model.Race;

/**
 * Defines a complete multiplayer match setup composing WorldParameters, Game, IslandConfig, and player info.
 */
public record MultiplayerSetup(
                               WorldParameters worldParameters,
                               Game game,
                               IslandConfig islandConfig,
                               Race player0Race,
                               int player0Team
) {
}
